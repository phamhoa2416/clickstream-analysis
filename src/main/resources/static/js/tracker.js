class ClickstreamTracker {
    constructor(config = {}) {
        this.defaultConfig = {
            endpoint: '/events/',
            appId: 'ecommerce-app',
            sampleRate: 1.0,
            sessionTimeout: 30 * 60 * 1000, // 30 minutes
            debug: true,
            schemaVersion: '1.0.0'
        };

        this.config = { ...this.defaultConfig, ...config };
        this.session_id = this.generateSessionId();
        this.user_id = this.getUserId();
        this.anonymous_id = this.getAnonymousId();
        this.session_start_time = new Date().toISOString();
        this.session_sequence = 0;
        this.lastActivityTime = Date.now();
        this.events = [];
        this.isSending = false;
        this.cache = new Map();
        this.shouldTrack = Math.random() < this.config.sampleRate;
        this.lastScrollDepth = 0;

        this.initialize();
        this.startSession();
        this.log('Tracker initialized');
    }

    generateSessionId() {
        return 'session_' + this.generateUUID().substring(0, 8);
    }

    getUserId() {
        const EXPIRY_DAYS = 30;
        const now = Date.now();
        const stored = JSON.parse(localStorage.getItem('user_id') || '{}');

        if (stored.id && stored.timestamp && now - stored.timestamp < EXPIRY_DAYS * 24 * 60 * 60 * 1000) {
            return stored.id;
        }

        const newId = 'user_' + this.generateUUID().substring(0, 8);
        localStorage.setItem('user_id', JSON.stringify({ id: newId, timestamp: now }));
        return newId;
    }

    getAnonymousId() {
        let anonymousId = localStorage.getItem('anonymous_id');
        if (!anonymousId) {
            anonymousId = 'anon_' + this.generateUUID().substring(0, 8);
            localStorage.setItem('anonymous_id', anonymousId);
        }
        return anonymousId;
    }

    generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => {
            const r = Math.random() * 16 | 0;
            const v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }

    getDeviceInfo() {
        const ua = navigator.userAgent;
        const getDeviceType = () => {
            if (/mobile/i.test(ua)) return 'mobile';
            if (/tablet/i.test(ua)) return 'tablet';
            return 'desktop';
        };

        return {
            platform: 'web',
            device_type: getDeviceType(),
            device_brand: /iPhone|iPad|Mac/.test(ua) ? 'Apple' : /Android/.test(ua) ? 'Android' : 'unknown',
            os_name: /Windows/.test(ua) ? 'Windows' : /Mac OS/.test(ua) ? 'macOS' : /Linux/.test(ua) ? 'Linux' : 'unknown',
            browser_name: /Chrome/.test(ua) ? 'Chrome' : /Firefox/.test(ua) ? 'Firefox' : /Safari/.test(ua) ? 'Safari' : 'unknown',
            browser_version: ua.match(/(?:Chrome|Firefox|Safari)\/([\d.]+)/)?.[1] || 'unknown',
            screen_resolution: `${window.screen.width}x${window.screen.height}`,
            user_agent: ua,
            language: navigator.language
        };
    }

    getPageInfo() {
        const url = new URL(window.location.href);
        const params = new URLSearchParams(url.search);
        let utmSource = params.get('utm_source');
        let utmMedium = params.get('utm_medium');
        let utmCampaign = params.get('utm_campaign');

        if (!utmSource && document.referrer) {
            const referrer = new URL(document.referrer);
            if (referrer.hostname.includes('google')) {
                utmSource = 'google';
                utmMedium = utmMedium || 'organic';
            } else if (referrer.hostname.includes('facebook')) {
                utmSource = 'facebook';
                utmMedium = utmMedium || 'social';
            }
        }

        return {
            page_url: window.location.href,
            page_path: window.location.pathname,
            page_referrer: document.referrer || 'direct',
            utm_source: utmSource || null,
            utm_medium: utmMedium || null,
            utm_campaign: utmCampaign || null,
            product_id: this.getProductIdFromPage()
        };
    }

    getProductIdFromPage() {
        const url = new URL(window.location.href);
        return url.searchParams.get('product_id') ||
            document.querySelector('meta[name="product_id"]')?.content || null;
    }

    getPerformanceInfo() {
        const navTiming = performance.getEntriesByType('navigation')[0] || {};
        return {
            page_load_time: navTiming.loadEventEnd ? (navTiming.loadEventEnd - navTiming.startTime) : null,
            time_to_interactive: navTiming.domInteractive ? (navTiming.domInteractive - navTiming.startTime) : null
        }
    }

    track(eventName, eventData = {}) {
        if (typeof eventName !== 'string' || !eventName.trim()) {
            this.log('Invalid event name:', eventName);
            return;
        }

        if (!this.shouldTrack) return;

        this.session_sequence++;
        const event = {
            event_id: this.generateUUID(),
            event_name: eventName,
            event_time: new Date().toISOString(),
            user_id: this.user_id,
            anonymous_id: this.anonymous_id,
            session_id: this.session_id,
            session_start_time: this.session_start_time,
            session_sequence: this.session_sequence,
            app_id: this.config.appId,
            schema_version: this.config.schemaVersion,
            ...this.getDeviceInfo(),
            ...this.getPageInfo(),
            ...this.getPerformanceInfo(),
            ...eventData
        };

        const eventHash = `${eventName}:${JSON.stringify(eventData)}`;
        if (this.cache.has(eventHash)) {
            this.log('Duplicate event:', event);
            return;
        }

        this.cache.set(eventHash, Date.now());
        if (this.cache.size > 500) {
            const oldestKey = this.cache.keys().next().value;
            this.cache.delete(oldestKey);
        }

        this.events.push(event);
        this.log("Event tracked: ", JSON.stringify(event, null, 2));

        if (['purchase', 'add_to_cart', 'checkout'].includes(eventName)) {
            this.sendEvents(true);
        } else if (['click', 'page_view', 'form_submit'].includes(eventName)) {
            this.sendEvents();
        } else {
            this.scheduleSendEvents();
        }
    }

    initialize() {
        this.track('page_view');

        document.addEventListener('click', (event) => {
            this.updateActivity();
            const target = event.target.closest('[data-track]');
            if (target) {
                this.track(target.dataset.track, {
                    element_id: target.id || null,
                    element_class: target.className || null,
                    element_text: target.textContent.trim().substring(0, 50) || null,
                    element_type: target.tagName.toLowerCase(),
                    element_name: target.getAttribute('name') || null,
                    product_id: target.dataset.productId || null,
                    product_category: target.dataset.productCategory || null,
                    order_id: target.dataset.orderId || null,
                    revenue: target.dataset.revenue ? parseFloat(target.dataset.revenue) : null,
                    currency: target.dataset.currency || null,
                    ...target.dataset
                });
            }
        });

        document.addEventListener('submit', (event) => {
            this.updateActivity();
            const target = event.target;
            if (target.dataset.track) {
                this.track(target.dataset.track, {
                    form_id: target.id || null,
                    form_class: target.className || null,
                    form_name: target.getAttribute('name') || null
                });
            }
        });

        window.addEventListener('scroll', this.throttle(() => {
            this.updateActivity();
            const scrollDepth = Math.round(
                (window.scrollY / (document.body.scrollHeight - window.innerHeight)) * 100
            );
            if (Math.abs(scrollDepth - this.lastScrollDepth) >= 25) {
                this.track('scroll', { scroll_depth: scrollDepth });
                this.lastScrollDepth = scrollDepth;
            }
        }, 2000));

        document.addEventListener('visibilitychange', () => {
            this.updateActivity();
            this.track('tab_change', {
                is_visible: !document.hidden,
                time_visible: document.hidden ? Date.now() - this.lastActivityTime : 0
            });
        });

        window.addEventListener('beforeunload', () => {
            this.updateActivity();
            this.track('page_exit');
            if (this.events.length > 0) {
                navigator.sendBeacon(
                    this.config.endpoint,
                    JSON.stringify({ events: this.events })
                );
                this.events = [];
            }
        });
    }

    startSession() {
        setInterval(() => {
            if (Date.now() - this.lastActivityTime > this.config.sessionTimeout) {
                this.session_id = this.generateSessionId();
                this.session_start_time = new Date().toISOString();
                this.session_sequence = 0;
                this.track('session_renew');
            }
        }, 60000);
    }

    async sendEvents(force = false) {
        if (this.isSending || (!force && this.events.length < 5)) return;

        this.isSending = true;
        const eventsToSend = [...this.events];
        this.events = [];

        try {
            const response = await fetch(this.config.endpoint, {
               method: 'POST',
               headers: {
                   'Content-Type': 'application/json',
                   'X-Clickstream-App-Id': this.config.appId,
                   'X-Request-ID': this.generateUUID()
               },
               body: JSON.stringify({ events: eventsToSend })
            });

            if (!response.ok) {
                throw new Error(`Failed to send events: ${response.status} ${response.statusText}`);
            }

            this.log(`Successfully sent ${eventsToSend.length} events`);
        } catch (error) {
            this.log('Error sending events:', error);
            this.events.unshift(...eventsToSend);
        } finally {
            this.isSending = false;
        }
    }

    scheduleSendEvents() {
        if (this.sendTimer) clearTimeout(this.sendTimer);
        this.sendTimer = setTimeout(() => this.sendEvents(), 3000);
    }

    updateActivity() {
        this.lastActivityTime = Date.now();
    }

    throttle(callback, delay) {
        let lastFunc;
        let lastRan = 0;
        return (...args) => {
            if (!lastRan) {
                callback.apply(this, args);
                lastRan = Date.now();
            } else {
                clearTimeout(lastFunc);
                lastFunc = setTimeout(() => {
                    if ((Date.now() - lastRan) >= delay) {
                        callback.apply(this, args);
                        lastRan = Date.now();
                    }
                }, delay - (Date.now() - lastRan));
            }
        };
    }

    log(...args) {
        if (this.config.debug) {
            console.log('[ClickstreamTracker]', ...args);
        }
    }
}

window.ClickstreamTracker = ClickstreamTracker;

if (window.ClickstreamConfig) {
    window.clickstreamTracker = new ClickstreamTracker(window.ClickstreamConfig);
}
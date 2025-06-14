class ClickstreamTracker {
    constructor(config = {}) {
        this.defaultConfig = {
            endpoint: '/events/',
            appId: 'ecommerce-app',
            sampleRate: 1.0,
            sessionTimeout: 30 * 60 * 1000,
            debug: true
        };

        this.config = { ...this.defaultConfig, ...config };
        this.session_id = this.generateSessionId();
        this.user_id = this.getUserId();
        this.lastActivityTime = Date.now();
        this.events = [];
        this.isSending = false;
        this.cache = new Set();
        this.shouldTrack = Math.random() < this.config.sampleRate;

        this.initialize();
        this.startSession();
        this.log('Tracker initialized');
    }

    generateSessionId() {
        return 'session_' + Math.random().toString(36).substring(2, 9);
    }

    getUserId() {
        const EXPIRY_DAYS = 30;
        const now = Date.now();
        const stored = JSON.parse(localStorage.getItem('user_id') || '{}');

        if (stored.id && stored.timestamp && now - stored.timestamp < EXPIRY_DAYS * 24 * 60 * 60 * 1000) {
            return stored.id;
        }

        const newId = 'user_' + Math.random().toString(36).substring(2, 9);
        const newInfo = { id: newId, timestamp: now };
        localStorage.setItem('user_id', JSON.stringify(newInfo));
        return newId;
    }

    generateUUID() {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(
            /[xy]/g, function(c) {
            const r = Math.random() * 16 | 0;
            const v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }

    getDeviceInfo() {
        return {
            platform: 'web',
            screen_resolution: `${window.screen.width}x${window.screen.height}`,
            viewport_size: `${window.innerWidth}x${window.innerHeight}`,
            user_agent: navigator.userAgent,
            language: navigator.language
        };
    }

    getPageInfo() {
        return {
            page_url: window.location.href,
            page_title: document.title,
            page_referrer: document.referrer || 'direct',
            page_path: window.location.pathname
        };
    }
    track(eventName, eventData = {}) {
        if (typeof eventName !== 'string' || !eventName.trim()) {
            this.log('Invalid event name:', eventName);
            return;
        }

        if (!this.shouldTrack) {
            return;
        }

        const event = {
            event_id: this.generateUUID(),
            event_name: eventName,
            event_time: new Date().toISOString(),
            user_id: this.user_id,
            session_id: this.session_id,
            app_id: this.config.app_id,
            ...this.getDeviceInfo(),
            ...this.getPageInfo(),
            ...eventData
        };

        const eventHash = JSON.stringify({event_name: eventName, ...eventData});
        if (this.cache.has(eventHash)) {
            this.log('Duplicate event:', event);
            return;
        }

        this.cache.add(eventHash);
        if (this.cache.size > 1000) {
            this.cache.delete(this.cache.values().next().value);
        }

        this.events.push(event);
        this.log("Event tracked: ", JSON.stringify(event, null, 2));

        if (['purchase', 'add_to_cart'].includes(eventName)) {
            this.sendEvents(true);
        } else if (['click', 'view', 'scroll'].includes(eventName)) {
            this.sendEvents();
        } else {
            this.scheduleSendEvents();
        }
    }

    initialize() {
        // Track page views
        this.track('page_view');

        // Track clicks
        document.addEventListener('click', (event) => {
            this.updateActivity();
            const target = event.target.closest('[data-track]');
            if (target) {
                this.track('click', {
                    element_id: target.id || null,
                    element_class: target.className || null,
                    element_text: target.textContent.trim().substring(0, 50),
                    element_type: target.tagName.toLowerCase(),
                    element_name: target.getAttribute('name') || null,
                    ...target.dataset
                });
            }
        });

        // Track form submissions
        document.addEventListener('submit', (event) => {
            this.updateActivity();
            this.track('form_submit', {
                form_id: event.target.id || null,
                form_class: event.target.className || null,
                form_name: event.target.getAttribute('name') || null
            });
        });

        // Track scroll
        window.addEventListener('scroll', this.throttle(() => {
            this.updateActivity();
            const scrollDepth = Math.round(
                (window.scrollY / (document.body.scrollHeight - window.innerHeight)) * 100
            );
            this.track('scroll', { scrollDepth: scrollDepth });
        }, 1000));

        // Track tab changes;
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
                const success =navigator.sendBeacon(
                    this.config.endpoint,
                    JSON.stringify({ events: this.events })
                );
                if (success) this.events = [];
            }
        });
    }

    startSession() {
        setInterval(() => {
            if (Date.now() - this.lastActivityTime > this.config.sessionTimeout) {
                this.session_id = this.generateSessionId();
                this.track('session_renew');
            }
        }, 60000);
    }

    async sendEvents(force = false) {
        if (this.isSending || (!force && this.events.length < 3)) return;

        this.isSending = true;
        const eventsToSend = [...this.events];
        this.events = [];

        try {
            const response = await fetch(this.config.endpoint, {
               method: 'POST',
               headers: {
                   'Content-Type': 'application/json',
                   'X-Clickstream-App-Id': this.config.appId
               },
               body: JSON.stringify({ events: eventsToSend })
            });

            if (!response.ok) {
                throw new Error(`Failed to send events: ${response.status} ${response.statusText}`);
            }

            this.log(`Successfully sent ${eventsToSend.length} events`);
        } catch (error) {
            console.error('Error sending events:', error);
            this.events.unshift(...eventsToSend);
        } finally {
            this.isSending = false;
        }
    }

    scheduleSendEvents() {
        if (this.sendTimer) clearTimeout(this.sendTimer);
        this.sendTimer = setTimeout(() => this.sendEvents(), 5000);
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
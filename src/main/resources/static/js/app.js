window.ClickstreamConfig = {
    endpoint: 'http://localhost/events/',
    appId: 'ecommerce-app',
    sampleRate: 1.0,
    debug: true,
    utmParams: ['source', 'medium', 'campaign', 'term', 'content']
};

document.addEventListener('DOMContentLoaded', () => {
    window.clickstreamTracker = new ClickstreamTracker(window.ClickstreamConfig);

    const urlParams = new URLSearchParams(window.location.search);
    const utmData = {};

    window.ClickstreamConfig.utmParams.forEach(param => {
        const value = urlParams.get(`utm_${param}`);
        if (value) utmData[`utm_${param}`] = value;
    });

    if (Object.keys(utmData).length > 0) {
        window.clickstreamTracker.track('utm_detected', utmData);
    }
})
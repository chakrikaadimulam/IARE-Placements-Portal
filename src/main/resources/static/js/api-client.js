(function () {
    const DEFAULT_GET_TIMEOUT = 15000; // 15s
    const DEFAULT_NON_GET_TIMEOUT = 30000; // 30s
    const DEFAULT_GET_RETRIES = 2;
    const DEFAULT_RETRY_DELAY = 1000;

    function delay(ms) {
        return new Promise(function (resolve) { setTimeout(resolve, ms); });
    }

    async function rawFetch(url, options, timeout) {
        const controller = new AbortController();
        const signal = controller.signal;
        const timer = setTimeout(function () { controller.abort(); }, timeout);

        try {
            const res = await fetch(url, Object.assign({}, options, { signal }));
            clearTimeout(timer);
            return res;
        } catch (err) {
            clearTimeout(timer);
            throw err;
        }
    }

    async function parseJsonSafe(response) {
        try {
            return await response.json();
        } catch (e) {
            return null;
        }
    }

    window.apiClient = {
        async get(url, opts) {
            opts = opts || {};
            const timeout = opts.timeout || DEFAULT_GET_TIMEOUT;
            const retries = typeof opts.retries === 'number' ? opts.retries : DEFAULT_GET_RETRIES;
            const retryDelay = typeof opts.retryDelay === 'number' ? opts.retryDelay : DEFAULT_RETRY_DELAY;

            let attempt = 0;
            while (true) {
                attempt++;
                try {
                    const res = await rawFetch(url, { method: 'GET', headers: opts.headers || {} }, timeout);
                    if (!res.ok) {
                        // treat common gateway/wakeup codes specially
                        if ([502, 503, 504, 524].includes(res.status)) {
                            const wakeErr = new Error('Server is waking up');
                            wakeErr.code = 'server_wake';
                            throw wakeErr;
                        }

                        const payload = await parseJsonSafe(res);
                        const msg = payload && payload.message ? payload.message : `Request failed (${res.status})`;
                        const err = new Error(msg);
                        err.status = res.status;
                        throw err;
                    }

                    const data = await parseJsonSafe(res);
                    if (data === null) throw new Error('Invalid JSON response');
                    return data;
                } catch (err) {
                    console.error('apiClient GET error', url, err);
                    const isNetwork = err.name === 'TypeError' || err.name === 'AbortError' || err.code === 'server_wake';
                    if (attempt <= retries && isNetwork) {
                        await delay(retryDelay);
                        continue;
                    }
                    throw err;
                }
            }
        },

        async post(url, body, opts) {
            opts = opts || {};
            const timeout = opts.timeout || DEFAULT_NON_GET_TIMEOUT;
            try {
                const res = await rawFetch(url, { method: 'POST', headers: Object.assign({ 'Content-Type': 'application/json' }, opts.headers || {}), body: JSON.stringify(body) }, timeout);
                const payload = await parseJsonSafe(res);
                if (!res.ok) {
                    const msg = payload && payload.message ? payload.message : `Request failed (${res.status})`;
                    const err = new Error(msg);
                    err.status = res.status;
                    throw err;
                }
                return payload;
            } catch (err) {
                console.error('apiClient POST error', url, err);
                throw err;
            }
        },

        async put(url, body, opts) {
            opts = opts || {};
            const timeout = opts.timeout || DEFAULT_NON_GET_TIMEOUT;
            try {
                const res = await rawFetch(url, { method: 'PUT', headers: Object.assign({ 'Content-Type': 'application/json' }, opts.headers || {}), body: JSON.stringify(body) }, timeout);
                const payload = await parseJsonSafe(res);
                if (!res.ok) {
                    const msg = payload && payload.message ? payload.message : `Request failed (${res.status})`;
                    const err = new Error(msg);
                    err.status = res.status;
                    throw err;
                }
                return payload;
            } catch (err) {
                console.error('apiClient PUT error', url, err);
                throw err;
            }
        },

        async delete(url, opts) {
            opts = opts || {};
            const timeout = opts.timeout || DEFAULT_NON_GET_TIMEOUT;
            try {
                const res = await rawFetch(url, { method: 'DELETE', headers: opts.headers || {} }, timeout);
                const payload = await parseJsonSafe(res);
                if (!res.ok) {
                    const msg = payload && payload.message ? payload.message : `Request failed (${res.status})`;
                    const err = new Error(msg);
                    err.status = res.status;
                    throw err;
                }
                return payload;
            } catch (err) {
                console.error('apiClient DELETE error', url, err);
                throw err;
            }
        },

        async cachedGet(cacheKey, url, ttl) {
            ttl = typeof ttl === 'number' ? ttl : 120000;
            try {
                const raw = sessionStorage.getItem(cacheKey);
                if (raw) {
                    try {
                        const parsed = JSON.parse(raw);
                        if (parsed && parsed.ts && (Date.now() - parsed.ts) < ttl && parsed.data) {
                            // background refresh
                            (async function () {
                                try {
                                    const fresh = await window.apiClient.get(url).catch(function (e) { throw e; });
                                    sessionStorage.setItem(cacheKey, JSON.stringify({ ts: Date.now(), data: fresh }));
                                } catch (e) {
                                    console.error('Background refresh failed for', url, e);
                                }
                            })();
                            return { data: parsed.data, fromCache: true };
                        }
                    } catch (e) {
                        console.error('Invalid cache data for', cacheKey, e);
                    }
                }

                const data = await window.apiClient.get(url);
                try {
                    sessionStorage.setItem(cacheKey, JSON.stringify({ ts: Date.now(), data: data }));
                } catch (e) {
                    // ignore storage set errors
                }
                return { data: data, fromCache: false };
            } catch (err) {
                // if cache exists return it
                const raw2 = sessionStorage.getItem(cacheKey);
                if (raw2) {
                    try {
                        const parsed2 = JSON.parse(raw2);
                        if (parsed2 && parsed2.data) return { data: parsed2.data, fromCache: true };
                    } catch (e) {
                        // fallthrough
                    }
                }
                throw err;
            }
        }
    };
})();

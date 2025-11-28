/**
 * JWT Token 자동 재발급 유틸리티
 */

let isRefreshing = false;
let failedQueue = [];

const getCookie = (name) => {
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
        const [cookieName, cookieValue] = cookie.trim().split('=');
        if (cookieName === name) {
            return cookieValue;
        }
    }
    return null;
};

const isUserLoggedIn = () => {
    const refreshToken = getCookie('refresh_token');
    return refreshToken !== null && refreshToken !== '';
};

const processQueue = (error, token = null) => {
    failedQueue.forEach(prom => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token);
        }
    });
    failedQueue = [];
};

const refreshAccessToken = async () => {
    if (!isUserLoggedIn()) {
        throw new Error('User not logged in');
    }

    try {
        const response = await fetch('/api/token', {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken: '' })
        });

        if (response.ok) {
            const data = await response.json();
            return data.accessToken;
        } else {
            throw new Error('Token refresh failed');
        }
    } catch (error) {
        throw error;
    }
};

const fetchWithAuth = async (url, options = {}) => {
    options.credentials = options.credentials || 'include';

    try {
        let response = await fetch(url, options);

        if (response.status === 401) {
            if (!isUserLoggedIn()) {
                return response;
            }

            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                }).then(() => {
                    return fetch(url, options);
                }).catch(() => {
                    return response;
                });
            }

            isRefreshing = true;

            try {
                await refreshAccessToken();
                processQueue(null, true);
                response = await fetch(url, options);
                return response;
            } catch (refreshError) {
                processQueue(refreshError, null);
                return response;
            } finally {
                isRefreshing = false;
            }
        }

        return response;
    } catch (error) {
        throw error;
    }
};

window.fetchWithAuth = fetchWithAuth;

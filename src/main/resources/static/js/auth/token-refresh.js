/**
 * JWT Token 자동 재발급 유틸리티
 * HttpOnly 쿠키 기반으로 동작 (서버에서 쿠키를 직접 읽음)
 */

let isRefreshing = false;
let failedQueue = [];

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

/**
 * Access Token 갱신 요청
 * HttpOnly 쿠키는 서버에서 직접 읽으므로 빈 body로 요청
 */
const refreshAccessToken = async () => {
    try {
        const response = await fetch('/api/token', {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({})
        });

        if (response.ok) {
            const data = await response.json();
            return data.accessToken;
        } else if (response.status === 401) {
            // Refresh Token 없거나 만료 - 조용히 실패 처리
            throw new Error('Token refresh failed - unauthorized');
        } else {
            throw new Error('Token refresh failed');
        }
    } catch (error) {
        throw error;
    }
};

/**
 * 인증이 필요한 API 호출용 fetch wrapper
 * 401 응답 시 자동으로 토큰 갱신 시도
 */
const fetchWithAuth = async (url, options = {}) => {
    options.credentials = options.credentials || 'include';

    try {
        let response = await fetch(url, options);

        // 401 응답이면 토큰 갱신 시도
        if (response.status === 401) {
            // 이미 갱신 중이면 대기열에 추가
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
                // 토큰 갱신 후 원래 요청 재시도
                response = await fetch(url, options);
                return response;
            } catch (refreshError) {
                processQueue(refreshError, null);
                // 토큰 갱신 실패 - 조용히 원래 401 응답 반환
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

/**
 * 서버에 인증 상태 확인 요청
 * HttpOnly 쿠키는 JS에서 읽을 수 없으므로 서버 API로 확인
 */
const checkAuthStatus = async () => {
    try {
        const response = await fetch('/api/auth/check', {
            method: 'POST',
            credentials: 'include'
        });
        if (response.ok) {
            return await response.json();
        }
        return { authenticated: false };
    } catch (error) {
        return { authenticated: false };
    }
};

window.fetchWithAuth = fetchWithAuth;
window.refreshAccessToken = refreshAccessToken;
window.checkAuthStatus = checkAuthStatus;

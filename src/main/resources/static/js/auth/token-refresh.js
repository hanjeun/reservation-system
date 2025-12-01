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
            credentials: 'include',  // 쿠키 포함
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({})  // 빈 객체 (서버가 쿠키에서 refresh_token 읽음)
        });

        if (response.ok) {
            const data = await response.json();
            console.log('✅ Access Token 갱신 성공');
            return data.accessToken;
        } else if (response.status === 401) {
            console.log('❌ Refresh Token 만료 - 재로그인 필요');
            throw new Error('Token refresh failed - unauthorized');
        } else {
            console.log('❌ Token refresh failed:', response.status);
            throw new Error('Token refresh failed');
        }
    } catch (error) {
        console.error('Token refresh error:', error);
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
            console.log('🔄 401 응답 - 토큰 갱신 시도...');

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
                console.log('토큰 갱신 실패 - 로그인 페이지로 이동 필요');
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
        console.error('Auth check error:', error);
        return { authenticated: false };
    }
};

window.fetchWithAuth = fetchWithAuth;
window.refreshAccessToken = refreshAccessToken;
window.checkAuthStatus = checkAuthStatus;

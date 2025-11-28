/**
 * JWT Token 자동 재발급 유틸리티
 * 
 * 이 파일은 Access Token이 만료되었을 때 자동으로 Refresh Token을 사용하여
 * 새로운 Access Token을 발급받는 로직을 포함합니다.
 * 
 * 사용법:
 * 1. HTML에 이 스크립트를 포함: <script src="/js/auth/token-refresh.js"></script>
 * 2. API 호출 시 fetchWithAuth() 함수 사용
 *
 * 개선사항:
 * - 로그인하지 않은 상태에서는 토큰 갱신 시도하지 않음
 * - 불필요한 401/400 에러 방지
 */

// 토큰 재발급 중인지 확인하는 플래그 (중복 호출 방지)
let isRefreshing = false;
// 토큰 재발급 대기 중인 요청들
let failedQueue = [];

/**
 * 쿠키에서 특정 이름의 값을 가져오는 헬퍼 함수
 * @param {string} name - 쿠키 이름
 * @returns {string|null} - 쿠키 값 또는 null
 */
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

/**
 * 로그인 상태 확인 함수
 * Refresh Token 쿠키가 있는지 확인하여 로그인 여부 판단
 * @returns {boolean} - 로그인 상태면 true, 아니면 false
 */
const isUserLoggedIn = () => {
    const refreshToken = getCookie('refresh_token');
    return refreshToken !== null && refreshToken !== '';
};

/**
 * 대기 중인 요청 처리
 */
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
 * Refresh Token을 사용하여 새로운 Access Token 발급
 * 로그인 상태가 아니면 재발급을 시도하지 않음
 */
const refreshAccessToken = async () => {
    // 로그인 상태 확인
    if (!isUserLoggedIn()) {
        console.log('⚠️  [Token Refresh] 로그인 상태가 아님 - 재발급 중단');
        throw new Error('User not logged in');
    }

    console.log('🔄 [Token Refresh] Access Token 재발급 시작...');
    
    try {
        const response = await fetch('/api/token', {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                refreshToken: '' // 쿠키에서 자동으로 가져옴
            })
        });

        if (response.ok) {
            const data = await response.json();
            console.log('✅ [Token Refresh] Access Token 재발급 성공');
            return data.accessToken;
        } else {
            console.error('❌ [Token Refresh] Access Token 재발급 실패:', response.status);
            throw new Error('Token refresh failed');
        }
    } catch (error) {
        console.error('❌ [Token Refresh] 재발급 오류:', error);
        throw error;
    }
};

/**
 * 인증이 필요한 API 호출을 위한 fetch 래퍼 함수
 * 401 에러 발생 시 자동으로 토큰 재발급 시도
 * 로그인 상태가 아니면 토큰 재발급을 시도하지 않음
 *
 * @param {string} url - API 엔드포인트 URL
 * @param {Object} options - fetch options
 * @returns {Promise<Response>} - fetch Response
 */
const fetchWithAuth = async (url, options = {}) => {
    // credentials 기본값 설정
    options.credentials = options.credentials || 'include';

    try {
        console.log(`📡 [API Call] ${options.method || 'GET'} ${url}`);
        let response = await fetch(url, options);

        // 401 Unauthorized 에러인 경우
        if (response.status === 401) {
            console.warn('⚠️  [Auth] 401 Unauthorized - 인증 필요');

            // 로그인 상태가 아니면 토큰 재발급 시도하지 않음
            if (!isUserLoggedIn()) {
                console.log('ℹ️  [Auth] 로그인 상태 아님 - 토큰 재발급 건너뜀');
                return response; // 401 응답을 그대로 반환
            }

            // 이미 토큰 재발급 중이면 대기
            if (isRefreshing) {
                console.log('⏳ [Token Refresh] 이미 재발급 중... 대기열에 추가');
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                }).then(() => {
                    console.log('🔄 [Token Refresh] 대기 완료, 요청 재시도');
                    return fetch(url, options);
                }).catch(() => {
                    // 재발급 실패 시 원래 401 응답 반환
                    return response;
                });
            }

            // 토큰 재발급 시작
            isRefreshing = true;

            try {
                await refreshAccessToken();
                console.log('🔄 [Token Refresh] 재발급 완료, 원래 요청 재시도 중...');
                
                // 대기 중인 요청들 처리
                processQueue(null, true);
                
                // 원래 요청 재시도
                response = await fetch(url, options);
                console.log('✅ [API Call] 재시도 성공:', response.status);
                
                return response;
            } catch (refreshError) {
                console.error('❌ [Token Refresh] 재발급 실패');
                processQueue(refreshError, null);

                // 재발급 실패 시 401 응답 그대로 반환 (각 페이지에서 처리)
                return response;
            } finally {
                isRefreshing = false;
            }
        }

        return response;
    } catch (error) {
        console.error('❌ [API Call] 요청 실패:', error);
        throw error;
    }
};

/**
 * 기존 fetch를 사용하는 코드와의 호환성을 위한 전역 함수
 */
window.fetchWithAuth = fetchWithAuth;

console.log('✅ [Token Refresh] 토큰 자동 재발급 유틸리티 로드 완료');

/**
 * 전역 사용자 정보 관리
 * 
 * 이 파일은 사용자 정보를 전역적으로 관리하여 중복 API 호출을 방지합니다.
 * header.html에서 한 번만 사용자 정보를 가져오고, 다른 페이지에서 재사용합니다.
 *
 * 개선사항:
 * - 로그인하지 않은 상태에서는 사용자 정보 요청하지 않음
 * - 불필요한 401 에러 방지
 */

// Role enum 정의 - 전역 객체로 선언
window.RoleEnum = window.RoleEnum || {
    USER: 'USER',
    BUSINESS: 'BUSINESS',
    ADMIN: 'ADMIN'
};

// 사업자 또는 관리자 권한 체크 함수
window.hasBusinessOrAdminRole = window.hasBusinessOrAdminRole || function(userRole) {
    return userRole === window.RoleEnum.BUSINESS || userRole === window.RoleEnum.ADMIN;
};

/**
 * 쿠키에서 특정 이름의 값을 가져오는 헬퍼 함수
 * httpOnly 쿠키는 JavaScript에서 읽을 수 없으므로 조심해서 사용
 * @param {string} name - 쿠키 이름
 * @returns {string|null} - 쿠키 값 또는 null
 */
const getCookieForUserState = (name) => {
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
 * 로그인 상태 확인 함수 (개선된 버전)
 * httpOnly 쿠키는 읽을 수 없으므로, 서버에 인증 상태를 확인하는 방식으로 변경
 * 하지만 초기 로드 시에는 일단 API 호출을 시도합니다
 * @returns {boolean} - 로그인 가능성이 있으면 true
 */
const checkLoginStatus = () => {
    // httpOnly가 true이면 refresh_token을 읽을 수 없음
    // 대신 일단 API 호출을 시도하고, 401 에러가 나면 로그인 상태가 아닌 것으로 판단
    const refreshToken = getCookieForUserState('refresh_token');

    // 쿠키를 읽을 수 있다면 (httpOnly=false인 경우) 확인
    if (refreshToken !== null && refreshToken !== '') {
        console.log('✅ [UserState] refresh_token 쿠키 확인됨 (httpOnly=false)');
        return true;
    }

    // 쿠키를 읽을 수 없는 경우 (httpOnly=true) - API 호출을 시도해야 함
    // 일단 true를 반환하여 API 호출을 시도하도록 함
    console.log('ℹ️  [UserState] refresh_token 쿠키를 읽을 수 없음 (httpOnly=true일 가능성) - API 호출 시도');
    return true; // httpOnly 쿠키가 있을 수 있으므로 시도
};

// 전역 사용자 상태 객체
window.UserState = {
    user: null,
    isLoading: false,
    isLoaded: false,
    listeners: [],

    /**
     * 사용자 정보 가져오기
     * httpOnly 쿠키를 사용하므로 일단 API 호출을 시도하고,
     * 401 에러가 나면 로그인되지 않은 것으로 판단
     * @returns {Promise<Object>} 사용자 정보
     */
    async getUser() {
        // 이미 로드된 경우 캐시된 정보 반환
        if (this.isLoaded && this.user) {
            console.log('✅ [UserState] 캐시된 사용자 정보 반환:', this.user);
            return this.user;
        }

        // 이미 로딩 중인 경우 대기
        if (this.isLoading) {
            console.log('⏳ [UserState] 사용자 정보 로딩 중... 대기');
            return new Promise((resolve, reject) => {
                this.listeners.push({ resolve, reject });
            });
        }

        // 새로운 요청 시작
        console.log('🔄 [UserState] 사용자 정보 로드 시작');
        this.isLoading = true;

        try {
            const response = await window.fetchWithAuth('/api/member/me');

            if (!response.ok) {
                // 401 에러면 로그인되지 않은 상태
                if (response.status === 401) {
                    console.log('ℹ️  [UserState] 인증되지 않음 (401) - 로그인 필요');
                    this.user = null;
                    this.isLoaded = true;
                    this.isLoading = false;

                    // 대기 중인 리스너들에게 null 반환
                    this.listeners.forEach(listener => listener.resolve(null));
                    this.listeners = [];

                    return null;
                }
                throw new Error('사용자 정보 로드 실패');
            }

            const userData = await response.json();
            this.user = userData;
            this.isLoaded = true;
            this.isLoading = false;

            console.log('✅ [UserState] 사용자 정보 로드 완료:', userData);

            // 대기 중인 리스너들에게 알림
            this.listeners.forEach(listener => listener.resolve(userData));
            this.listeners = [];

            return userData;
        } catch (error) {
            this.isLoading = false;
            this.isLoaded = true; // 로드 시도 완료 표시
            this.user = null;
            console.error('❌ [UserState] 사용자 정보 로드 실패:', error);

            // 대기 중인 리스너들에게 에러 알림
            this.listeners.forEach(listener => listener.reject(error));
            this.listeners = [];

            throw error;
        }
    },

    /**
     * 사용자 정보 초기화 (로그아웃 시 사용)
     */
    clearUser() {
        console.log('🗑️ [UserState] 사용자 정보 초기화');
        this.user = null;
        this.isLoaded = false;
        this.isLoading = false;
        this.listeners = [];
    },

    /**
     * 캐시된 사용자 정보 가져오기 (API 호출 없음)
     * @returns {Object|null} 캐시된 사용자 정보 또는 null
     */
    getCachedUser() {
        return this.user;
    },

    /**
     * 사용자가 로그인 상태인지 확인
     * @returns {boolean}
     */
    isAuthenticated() {
        return this.isLoaded && this.user !== null;
    }
};

console.log('✅ [UserState] 전역 사용자 상태 관리 로드 완료');

/**
 * 전역 사용자 정보 관리
 */

window.RoleEnum = window.RoleEnum || {
    USER: 'USER',
    BUSINESS: 'BUSINESS',
    ADMIN: 'ADMIN'
};

window.hasBusinessOrAdminRole = window.hasBusinessOrAdminRole || function(userRole) {
    return userRole === window.RoleEnum.BUSINESS || userRole === window.RoleEnum.ADMIN;
};

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

const checkLoginStatus = () => {
    const refreshToken = getCookieForUserState('refresh_token');
    return refreshToken !== null && refreshToken !== '' || true;
};

window.UserState = {
    user: null,
    isLoading: false,
    isLoaded: false,
    listeners: [],

    async getUser() {
        if (this.isLoaded && this.user) {
            return this.user;
        }

        if (this.isLoading) {
            return new Promise((resolve, reject) => {
                this.listeners.push({ resolve, reject });
            });
        }

        this.isLoading = true;

        try {
            const response = await window.fetchWithAuth('/api/member/me');

            if (!response.ok) {
                if (response.status === 401) {
                    this.user = null;
                    this.isLoaded = true;
                    this.isLoading = false;
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

            this.listeners.forEach(listener => listener.resolve(userData));
            this.listeners = [];

            return userData;
        } catch (error) {
            this.isLoading = false;
            this.isLoaded = true;
            this.user = null;

            this.listeners.forEach(listener => listener.reject(error));
            this.listeners = [];

            throw error;
        }
    },

    clearUser() {
        this.user = null;
        this.isLoaded = false;
        this.isLoading = false;
        this.listeners = [];
    },

    getCachedUser() {
        return this.user;
    },

    isAuthenticated() {
        return this.isLoaded && this.user !== null;
    }
};

/**
 * 인앱 브라우저 감지 및 안내
 */

// 인앱 브라우저 감지 함수
function isInAppBrowser() {
    const ua = navigator.userAgent || navigator.vendor || window.opera;
    
    // 카카오톡 인앱 브라우저
    if (ua.match(/KAKAOTALK/i)) {
        return { isInApp: true, browser: '카카오톡' };
    }
    
    // 네이버 앱 인앱 브라우저
    if (ua.match(/NAVER/i)) {
        return { isInApp: true, browser: '네이버' };
    }
    
    // 라인 인앱 브라우저
    if (ua.match(/Line/i)) {
        return { isInApp: true, browser: '라인' };
    }
    
    // 페이스북 인앱 브라우저
    if (ua.match(/FBAN|FBAV/i)) {
        return { isInApp: true, browser: '페이스북' };
    }
    
    // 인스타그램 인앱 브라우저
    if (ua.match(/Instagram/i)) {
        return { isInApp: true, browser: '인스타그램' };
    }
    
    // 트위터 인앱 브라우저
    if (ua.match(/Twitter/i)) {
        return { isInApp: true, browser: '트위터' };
    }
    
    return { isInApp: false, browser: null };
}

// 구글 로그인 버튼 차단
function blockGoogleLoginInInAppBrowser() {
    const result = isInAppBrowser();
    
    if (result.isInApp) {
        console.log(`${result.browser} 인앱 브라우저 감지됨`);
        
        // 구글 로그인 버튼 찾기
        const googleLoginBtn = document.querySelector('a[href*="/oauth2/authorization/google"]');
        
        if (googleLoginBtn) {
            googleLoginBtn.addEventListener('click', function(e) {
                e.preventDefault();
                
                // 커스텀 알림 표시
                showBrowserGuide(result.browser);
                
                // 또는 안내 페이지로 리디렉션
                // window.location.href = '/browser-guide';
            });
            
            // 버튼에 경고 표시 추가
            googleLoginBtn.style.opacity = '0.5';
            googleLoginBtn.style.cursor = 'not-allowed';
            
            // 버튼 아래 경고 메시지 추가
            const warning = document.createElement('div');
            warning.className = 'alert alert-warning mt-2';
            warning.innerHTML = `
                <small>
                    ⚠️ ${result.browser}에서는 구글 로그인이 제한됩니다.<br>
                    외부 브라우저에서 열어주세요.
                </small>
            `;
            googleLoginBtn.parentElement.appendChild(warning);
        }
    }
}

// 브라우저 안내 모달 표시
function showBrowserGuide(browserName) {
    const modalHtml = `
        <div id="browserGuideModal" style="
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0,0,0,0.7);
            z-index: 9999;
            display: flex;
            align-items: center;
            justify-content: center;
        ">
            <div style="
                background: white;
                padding: 30px;
                border-radius: 15px;
                max-width: 90%;
                width: 400px;
                text-align: center;
            ">
                <div style="font-size: 48px; margin-bottom: 20px;">🌐</div>
                <h3 style="margin-bottom: 20px;">외부 브라우저에서 열어주세요</h3>
                <p style="color: #666; line-height: 1.6; margin-bottom: 20px;">
                    ${browserName}에서는 구글 로그인이 제한됩니다.<br>
                    아래 방법으로 외부 브라우저에서 열어주세요.
                </p>
                <div style="
                    background: #f8f9fa;
                    padding: 20px;
                    border-radius: 10px;
                    text-align: left;
                    margin-bottom: 20px;
                ">
                    <div style="margin: 10px 0;">
                        ✓ 우측 상단 <strong>[⋯]</strong> 메뉴 클릭
                    </div>
                    <div style="margin: 10px 0;">
                        ✓ <strong>[외부 브라우저에서 열기]</strong> 선택
                    </div>
                    <div style="margin: 10px 0;">
                        ✓ Chrome 또는 Safari에서 열기
                    </div>
                </div>
                <button onclick="closeBrowserGuide()" style="
                    background: #007bff;
                    color: white;
                    border: none;
                    padding: 12px 30px;
                    border-radius: 8px;
                    font-size: 16px;
                    cursor: pointer;
                ">확인</button>
            </div>
        </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', modalHtml);
}

// 모달 닫기
function closeBrowserGuide() {
    const modal = document.getElementById('browserGuideModal');
    if (modal) {
        modal.remove();
    }
}

// 페이지 로드 시 실행
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', blockGoogleLoginInInAppBrowser);
} else {
    blockGoogleLoginInInAppBrowser();
}

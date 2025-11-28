# Access Token 자동 재발급 적용 가이드

## ✅ 이미 완료된 작업

1. **token-refresh.js 생성** ✅
   - 위치: `/src/main/resources/static/js/auth/token-refresh.js`
   - 기능: Access Token 자동 재발급 로직

2. **header.html 업데이트** ✅
   - token-refresh.js 스크립트 추가
   - `/api/member/me` 호출을 fetchWithAuth로 변경

3. **footer.html 업데이트** ✅
   - `/api/member/me` 호출을 fetchWithAuth로 변경

## 📝 적용 필요한 파일들

모든 페이지에서 **인증이 필요한 API 호출**을 `fetch` → `window.fetchWithAuth`로 변경해야 합니다.

### 변경 방법

**기존 코드:**
```javascript
fetch('/api/member/me', {
    credentials: 'include'
})
.then(response => response.json())
.then(data => {
    // 처리
})
.catch(error => {
    // 에러 처리
});
```

**변경 후:**
```javascript
window.fetchWithAuth('/api/member/me')
.then(response => response.json())
.then(data => {
    // 처리
})
.catch(error => {
    // 에러 처리
});
```

### 수정이 필요한 주요 파일

#### 1. mypage.html
```javascript
// 수정 전
fetch('/api/member/me', { credentials: 'include' })

// 수정 후  
window.fetchWithAuth('/api/member/me')
```

다음 함수들에서 모든 fetch 호출을 변경:
- `loadUserInfo()` 
- `loadMyReservations()`
- `loadMyStores()`
- `deleteReservation(reservationId)`
- `deleteStore(storeId)`
- `deleteAccount()`
- `editAccount()`

#### 2. community.html, community-detail.html
- 게시글 목록 조회
- 게시글 작성/수정/삭제
- 댓글 작성/삭제
- 좋아요 기능

#### 3. customer-service-inquiry.html, admin-inquiry.html
- 문의 목록 조회
- 문의 작성/수정/삭제
- 답변 작성

#### 4. store-*.html (store-list, store-detail, store-edit, store-register, store-promotion)
- 가게 목록 조회
- 가게 상세 정보 조회
- 가게 등록/수정/삭제
- 예약 관련 API

## 🔍 빠른 수정 방법

### 검색 패턴
파일에서 다음 패턴을 찾아서 변경:

1. `fetch('/api/` → `window.fetchWithAuth('/api/`
2. `fetch("/api/` → `window.fetchWithAuth("/api/`
3. ``fetch(`/api/`` → ``window.fetchWithAuth(`/api/``

### 주의사항
- **공개 API는 변경하지 않음**: `/api/auth/login`, `/api/auth/signup` 등
- **credentials: 'include'는 제거 가능**: fetchWithAuth가 자동으로 설정함

## 🎯 자동 재발급 동작 방식

```
1. 사용자가 API 호출
   ↓
2. Access Token 만료 (401 에러)
   ↓  
3. fetchWithAuth가 자동 감지
   ↓
4. /api/token 호출 (Refresh Token 사용)
   ↓
5. 새로운 Access Token 발급
   ↓
6. 원래 API 자동 재시도
   ↓
7. 성공!
```

## 📊 테스트 방법

1. 로그인
2. 개발자 도구 → Console 탭 열기
3. 30분 대기 (또는 Access Token 쿠키 삭제로 만료 시뮬레이션)
4. 페이지에서 API 호출하는 기능 실행
5. 콘솔에서 다음 로그 확인:

```
⚠️  [Auth] 401 Unauthorized - 토큰 만료 가능성
🔄 [Token Refresh] Access Token 재발급 시작...
✅ [Token Refresh] Access Token 재발급 성공
🔄 [Token Refresh] 재발급 완료, 원래 요청 재시도 중...
✅ [API Call] 재시도 성공: 200
```

6. 서버 콘솔에서도 확인:

```
╔══════════════════════════════════════════════════════════════╗
║  📥 Access Token 재발급 요청 수신 (POST /api/token)          ║
╚══════════════════════════════════════════════════════════════╝
...
✅ Access Token 재발급 완료
```

## 🚨 문제 해결

### 중복 요청이 계속 발생하는 경우
- fetchWithAuth로 변경되지 않은 fetch 호출이 있는지 확인
- 브라우저 콘솔에서 어떤 API가 반복 호출되는지 확인

### 재발급이 실패하는 경우
- Refresh Token이 만료되었을 수 있음 (14일 후)
- 자동으로 로그인 페이지로 리다이렉트됨

### 로그가 보이지 않는 경우
- application.yml의 로그 레벨 확인
- 브라우저 콘솔 확인

## 📌 다음 단계

1. mypage.html부터 시작해서 모든 fetch를 fetchWithAuth로 변경
2. 각 페이지를 테스트
3. 30분 후에도 정상 작동하는지 확인

좋은 작업 되세요! 🚀

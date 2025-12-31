# Flatpickr 라이브러리 로컬 설치

이 폴더에 다음 파일들을 다운로드하여 저장하세요:

## 필요한 파일 목록

| 파일명 | 다운로드 링크 |
|--------|--------------|
| `flatpickr.min.css` | https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.css |
| `airbnb.css` | https://cdn.jsdelivr.net/npm/flatpickr/dist/themes/airbnb.css |
| `flatpickr.min.js` | https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.js |
| `ko.js` | https://cdn.jsdelivr.net/npm/flatpickr/dist/l10n/ko.js |

## 다운로드 방법

### 방법 1: 브라우저에서 직접 다운로드
1. 위 링크를 브라우저에서 열기
2. Ctrl+S (또는 Cmd+S)로 저장
3. 이 폴더에 저장

### 방법 2: 명령어로 다운로드 (인터넷 연결 필요)
```bash
cd src/main/resources/static/vendor/flatpickr

curl -O https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.css
curl -O https://cdn.jsdelivr.net/npm/flatpickr/dist/themes/airbnb.css
curl -O https://cdn.jsdelivr.net/npm/flatpickr/dist/flatpickr.min.js
curl -O https://cdn.jsdelivr.net/npm/flatpickr/dist/l10n/ko.js
```

### 방법 3: npm으로 설치 후 복사
```bash
npm install flatpickr
# 그 후 node_modules/flatpickr/dist 폴더에서 파일 복사
```

## 파일 구조
```
src/main/resources/static/vendor/flatpickr/
├── flatpickr.min.css   (CSS 메인)
├── airbnb.css          (테마)
├── flatpickr.min.js    (JS 메인)
├── ko.js               (한국어 로케일)
└── README.md           (이 파일)
```

## 버전 정보
- Flatpickr: 4.x (최신 버전 권장)
- 다운로드 날짜: 2025년

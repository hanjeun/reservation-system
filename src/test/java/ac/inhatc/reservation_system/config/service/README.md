# JWT Refresh Token 테스트 가이드

## 📋 개요

이 테스트는 **Refresh Token을 이용한 Access Token 재발급** 로직이 제대로 작동하는지 검증합니다.

## 🎯 테스트 목적

현재 시스템에서는 다음과 같은 JWT 토큰 메커니즘을 사용합니다:
- **Access Token**: 30분 유효 (API 요청 시 사용)
- **Refresh Token**: 14일 유효 (Access Token 재발급에 사용)

Access Token이 만료되면 Refresh Token을 사용하여 새로운 Access Token을 발급받아 **자동 로그아웃을 방지**합니다.

## 🧪 테스트 항목

### TokenServiceTest.java

1. **유효한 Refresh Token으로 Access Token 재발급 성공**
   - Refresh Token이 유효한 경우 새로운 Access Token이 정상적으로 발급되는지 확인
   - 발급된 Access Token이 유효한지 검증
   - Access Token에서 사용자 정보를 정확히 추출할 수 있는지 확인

2. **존재하지 않는 Refresh Token으로 재발급 시도 시 실패**
   - DB에 없는 Refresh Token으로 재발급 시도 시 예외 발생 확인
   - `IllegalArgumentException` 예외 확인

3. **만료된 Refresh Token으로 재발급 시도 시 실패**
   - 만료된 Refresh Token으로 재발급 시도 시 예외 발생 확인
   - `IllegalAccessException` 예외 확인

4. **여러 번 Access Token 재발급 가능 (Refresh Token 재사용)**
   - 동일한 Refresh Token으로 여러 번 Access Token을 재발급할 수 있는지 확인
   - Refresh Token이 소진되지 않고 재사용 가능한지 검증

5. **재발급된 Access Token이 사용자의 Role 정보를 포함하는지 확인**
   - Access Token에 사용자의 권한(Role) 정보가 포함되어 있는지 확인
   - 권한 기반 접근 제어가 정상 작동하는지 검증

## 🚀 테스트 실행 방법

### 방법 1: IntelliJ에서 실행

1. `TokenServiceTest.java` 파일 열기
2. 클래스 또는 개별 테스트 메서드 옆의 초록색 ▶️ 아이콘 클릭
3. "Run 'TokenServiceTest'" 선택

### 방법 2: Gradle을 통한 실행

터미널에서 다음 명령어 실행:

```bash
# Windows
gradlew.bat test --tests TokenServiceTest

# Mac/Linux
./gradlew test --tests TokenServiceTest
```

### 방법 3: 전체 테스트 실행

```bash
# Windows
gradlew.bat test

# Mac/Linux
./gradlew test
```

## 📊 테스트 결과 확인

테스트가 성공하면 콘솔에 다음과 같은 상세 로그가 출력됩니다:

```
========== 테스트 설정 완료 ==========
테스트 회원 ID: 1
테스트 회원 이메일: test@example.com
Refresh Token 생성 완료
Refresh Token 만료 시간: 14일
=====================================

========== 테스트 시작: 유효한 Refresh Token으로 Access Token 재발급 ==========
사용할 Refresh Token: eyJhbGciOiJIUzI1NiI...

[1단계] Refresh Token으로 새로운 Access Token 생성 중...
✅ 새로운 Access Token 생성 완료: eyJhbGciOiJIUzI1NiI...

[2단계] 생성된 Access Token 검증 중...
✅ Access Token 유효성 검증 완료

[3단계] Access Token에서 사용자 정보 추출 중...
✅ 추출된 User ID: 1
✅ 예상 User ID: 1
✅ User ID 일치 확인 완료

[4단계] Access Token으로 회원 정보 조회 중...
✅ 조회된 회원 이메일: test@example.com
✅ 회원 정보 일치 확인 완료

========== ✅ 테스트 성공: Refresh Token을 이용한 Access Token 재발급 완료 ==========
```

## 📁 테스트 관련 파일

```
src/
├── main/
│   └── java/
│       └── ac/inhatc/reservation_system/
│           └── config/
│               ├── service/
│               │   ├── TokenService.java          # 토큰 재발급 로직
│               │   └── RefreshTokenService.java   # Refresh Token 조회
│               └── jwt/
│                   └── TokenProvider.java         # JWT 생성/검증
└── test/
    ├── java/
    │   └── ac/inhatc/reservation_system/
    │       └── config/
    │           └── service/
    │               └── TokenServiceTest.java      # 📌 테스트 파일
    └── resources/
        └── application-test.yml                    # 테스트 설정
```

## 🔍 주요 검증 포인트

### 1. Refresh Token → Access Token 재발급 흐름

```
1. 사용자가 로그인
   ↓
2. Access Token (30분) + Refresh Token (14일) 발급
   ↓
3. Access Token 만료 (30분 후)
   ↓
4. Refresh Token을 사용하여 새로운 Access Token 발급
   ↓
5. 사용자는 로그아웃 없이 계속 사용 가능
```

### 2. 보안 체크

- ✅ 만료된 Refresh Token은 사용 불가
- ✅ 존재하지 않는 Refresh Token은 사용 불가
- ✅ 유효한 Refresh Token만 Access Token 재발급 가능
- ✅ 재발급된 Access Token에 사용자 정보 포함

## 💡 테스트 실행 시 주의사항

1. **데이터베이스**: 테스트는 H2 인메모리 DB를 사용하므로 MySQL 연결 불필요
2. **독립성**: 각 테스트는 독립적으로 실행되며 서로 영향을 주지 않음
3. **트랜잭션**: `@Transactional`로 각 테스트 후 데이터 자동 롤백

## 🎓 배운 점

이 테스트를 통해 다음을 확인할 수 있습니다:

1. ✅ **Refresh Token 재발급 로직이 정상 작동**
2. ✅ **자동 로그아웃 방지 메커니즘이 구현됨**
3. ✅ **보안 예외 처리가 올바르게 동작**
4. ✅ **토큰에 사용자 권한 정보가 포함됨**

## 🚨 문제 해결

### 테스트 실패 시

1. **의존성 확인**: `build.gradle`에 H2 데이터베이스가 추가되었는지 확인
2. **설정 확인**: `application-test.yml` 파일이 존재하는지 확인
3. **JWT Secret Key**: 64자 이상의 시크릿 키가 설정되었는지 확인

### 빌드 오류 시

```bash
# Gradle 의존성 재다운로드
gradlew.bat clean build --refresh-dependencies
```

## 📚 참고 자료

- Spring Security JWT 문서
- JWT.io - JWT 토큰 디버깅
- Spring Boot Testing 가이드

package ac.inhatc.reservation_system.config.controller;

import ac.inhatc.reservation_system.config.dto.CreateAccessTokenRequest;
import ac.inhatc.reservation_system.config.dto.CreateAccessTokenResponse;
import ac.inhatc.reservation_system.config.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
@RestController
public class TokenApiController {

    private final TokenService tokenService;
    private static final Duration ACCESS_TOKEN_DURATION = Duration.ofHours(2);

    @PostMapping("/api/token")
    public ResponseEntity<CreateAccessTokenResponse> createNewAccessToken(
            @RequestBody CreateAccessTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        log.info("\n╔══════════════════════════════════════════════════════════════╗");
        log.info("║  📥 Access Token 재발급 요청 수신 (POST /api/token)          ║");
        log.info("╚══════════════════════════════════════════════════════════════╝");
        
        try {
            // 1. Request Body 또는 쿠키에서 Refresh Token 가져오기
            log.info("🔍 Refresh Token 추출 중...");
            String refreshToken = request.getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                log.info("   - Request Body에 Refresh Token 없음, 쿠키에서 조회 시도");
                refreshToken = getRefreshTokenFromCookie(httpRequest);
            } else {
                log.info("   - Request Body에서 Refresh Token 발견");
            }

            if (refreshToken == null) {
                log.warn("⚠️  Refresh Token을 찾을 수 없음 (로그인 필요)");
                log.info("╔══════════════════════════════════════════════════════════════╗");
                log.info("║  ⚠️  Access Token 재발급 실패 - Refresh Token 없음 (401)   ║");
                log.info("╚══════════════════════════════════════════════════════════════╝\n");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            log.info("✅ Refresh Token 추출 완료: {}...", refreshToken.substring(0, Math.min(20, refreshToken.length())));

            // 2. 새로운 Access Token 생성
            log.info("🔄 TokenService를 통한 Access Token 재발급 시작...");
            String newAccessToken = tokenService.createNewAccessToken(refreshToken);
            
            // 3. 쿠키에 저장
            log.info("🍪 새로운 Access Token을 쿠키에 저장 중...");
            addAccessTokenToCookie(httpResponse, newAccessToken);
            log.info("✅ Access Token이 쿠키에 저장됨 (만료시간: {}시간)", ACCESS_TOKEN_DURATION.toHours());

            log.info("╔══════════════════════════════════════════════════════════════╗");
            log.info("║  ✅ Access Token 재발급 성공 (201 Created)                   ║");
            log.info("╚══════════════════════════════════════════════════════════════╝\n");
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new CreateAccessTokenResponse(newAccessToken));

        } catch (IllegalAccessException e) {
            log.error("╔══════════════════════════════════════════════════════════════╗");
            log.error("║  ❌ Access Token 재발급 실패 - 인증 오류 (401 Unauthorized) ║");
            log.error("╚══════════════════════════════════════════════════════════════╝");
            log.error("오류 메시지: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("╔══════════════════════════════════════════════════════════════╗");
            log.error("║  ❌ Access Token 재발급 실패 - 서버 오류 (500 Internal)     ║");
            log.error("╚══════════════════════════════════════════════════════════════╝");
            log.error("오류 상세:", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> "refresh_token".equals(cookie.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        return null;
    }

    private void addAccessTokenToCookie(HttpServletResponse response, String accessToken) {
        Cookie cookie = new Cookie("access_token", accessToken);
        cookie.setHttpOnly(false); // JavaScript에서 접근 가능하도록 설정
        cookie.setSecure(false);  // 개발: false, 배포: true
        cookie.setPath("/");
        cookie.setMaxAge((int) ACCESS_TOKEN_DURATION.toSeconds());
        response.addCookie(cookie);
    }
}

package ac.inhatc.reservation_system.config.controller;

import ac.inhatc.reservation_system.config.dto.CreateAccessTokenRequest;
import ac.inhatc.reservation_system.config.dto.CreateAccessTokenResponse;
import ac.inhatc.reservation_system.config.jwt.JwtProperties;
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

import java.util.Arrays;

@Slf4j
@RequiredArgsConstructor
@RestController
public class TokenApiController {

    private final TokenService tokenService;
    private final JwtProperties jwtProperties;

    @PostMapping("/api/token")
    public ResponseEntity<CreateAccessTokenResponse> createNewAccessToken(
            @RequestBody(required = false) CreateAccessTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        try {
            // Request Body 또는 쿠키에서 Refresh Token 가져오기
            String refreshToken = null;
            if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isEmpty()) {
                refreshToken = request.getRefreshToken();
            }
            if (refreshToken == null) {
                refreshToken = getRefreshTokenFromCookie(httpRequest);
            }

            if (refreshToken == null) {
                log.warn("토큰 재발급 실패 - Refresh Token 없음");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            log.info("토큰 재발급 요청 - Refresh Token: {}...", refreshToken.substring(0, Math.min(20, refreshToken.length())));

            // 새로운 Access Token 생성
            String newAccessToken = tokenService.createNewAccessToken(refreshToken);

            // 쿠키에 저장 (로그인 시와 동일한 설정 사용)
            addAccessTokenToCookie(httpResponse, newAccessToken);

            log.info("토큰 재발급 성공");

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new CreateAccessTokenResponse(newAccessToken));

        } catch (IllegalAccessException e) {
            log.warn("토큰 재발급 실패 - 인증 오류: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (IllegalArgumentException e) {
            log.warn("토큰 재발급 실패 - DB에서 토큰 못찾음: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("토큰 재발급 실패 - 서버 오류", e);
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
        cookie.setHttpOnly(true);  // 로그인 시와 동일하게 HttpOnly 설정
        cookie.setPath("/");
        cookie.setMaxAge((int) jwtProperties.getAccessTokenExpiration().toSeconds());
        response.addCookie(cookie);
    }
}

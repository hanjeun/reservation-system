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
        try {
            // Request Body 또는 쿠키에서 Refresh Token 가져오기
            String refreshToken = request.getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                refreshToken = getRefreshTokenFromCookie(httpRequest);
            }

            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 새로운 Access Token 생성
            String newAccessToken = tokenService.createNewAccessToken(refreshToken);
            
            // 쿠키에 저장
            addAccessTokenToCookie(httpResponse, newAccessToken);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new CreateAccessTokenResponse(newAccessToken));

        } catch (IllegalAccessException e) {
            log.warn("토큰 재발급 실패 - 인증 오류: {}", e.getMessage());
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
        cookie.setHttpOnly(false);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge((int) ACCESS_TOKEN_DURATION.toSeconds());
        response.addCookie(cookie);
    }
}

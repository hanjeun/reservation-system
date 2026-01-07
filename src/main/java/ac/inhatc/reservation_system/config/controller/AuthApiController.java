package ac.inhatc.reservation_system.config.controller;

import ac.inhatc.reservation_system.config.jwt.JwtProperties;
import ac.inhatc.reservation_system.config.jwt.TokenProvider;
import ac.inhatc.reservation_system.config.jwt.repository.RefreshTokenRepository;
import ac.inhatc.reservation_system.member.entity.AuthProvider;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.repository.MemberRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final MemberRepository memberRepository;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest, HttpServletResponse response) {
        try {
            String email = loginRequest.get("email");
            String password = loginRequest.get("password");

            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

            // OAuth 사용자가 일반 로그인을 시도하는 경우
            if (member.isOAuthUser()) {
                String providerName = member.getProvider().name();
                return ResponseEntity.status(401).body(Map.of(
                    "error", providerName + " 계정으로 가입된 사용자입니다. " + providerName + " 로그인을 이용해주세요."
                ));
            }

            // 비밀번호가 없는 경우 (OAuth로만 가입한 경우)
            if (member.getPassword() == null) {
                return ResponseEntity.status(401).body(Map.of(
                    "error", "소셜 로그인으로 가입된 계정입니다. 소셜 로그인을 이용해주세요."
                ));
            }

            if (!passwordEncoder.matches(password, member.getPassword())) {
                return ResponseEntity.status(401).body(Map.of("error", "비밀번호가 일치하지 않습니다."));
            }

            String accessToken = tokenProvider.generateAccessToken(member);
            String refreshToken = tokenProvider.generateRefreshToken(member);

            addTokenCookie(response, "access_token", accessToken,
                (int) jwtProperties.getAccessTokenExpiration().toSeconds());
            addTokenCookie(response, "refresh_token", refreshToken,
                (int) jwtProperties.getRefreshTokenExpiration().toSeconds());

            log.info("로그인 성공: email={}", email);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "user", Map.of(
                    "id", member.getId(),
                    "name", member.getName(),
                    "email", member.getEmail()
                )
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("로그인 중 오류 발생", e);
            return ResponseEntity.status(500).body(Map.of("error", "로그인 처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("refresh_token".equals(cookie.getName())) {
                        String refreshToken = cookie.getValue();
                        refreshTokenRepository.findByRefreshToken(refreshToken)
                                .ifPresent(refreshTokenRepository::delete);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("로그아웃 중 토큰 삭제 실패: {}", e.getMessage());
        }

        // JWT 토큰 쿠키 삭제
        Cookie accessCookie = new Cookie("access_token", null);
        accessCookie.setMaxAge(0);
        accessCookie.setPath("/");
        accessCookie.setHttpOnly(true);
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refresh_token", null);
        refreshCookie.setMaxAge(0);
        refreshCookie.setPath("/");
        refreshCookie.setHttpOnly(true);
        response.addCookie(refreshCookie);

        // JSESSIONID 쿠키 삭제
        Cookie jsessionCookie = new Cookie("JSESSIONID", null);
        jsessionCookie.setMaxAge(0);
        jsessionCookie.setPath("/");
        jsessionCookie.setHttpOnly(true);
        response.addCookie(jsessionCookie);

        log.info("🚪 로그아웃 완료 - 모든 인증 쿠키 삭제됨");

        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/check")
    public ResponseEntity<?> checkAuth(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String accessToken = null;
        String refreshToken = null;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    accessToken = cookie.getValue();
                } else if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }

        boolean hasValidAccessToken = accessToken != null && tokenProvider.validToken(accessToken);
        boolean hasValidRefreshToken = refreshToken != null && tokenProvider.validToken(refreshToken);

        // Refresh Token이 DB에 존재하는지도 확인
        boolean refreshTokenInDb = false;
        if (hasValidRefreshToken) {
            refreshTokenInDb = refreshTokenRepository.findByRefreshToken(refreshToken).isPresent();
        }

        return ResponseEntity.ok(Map.of(
            "hasAccessToken", accessToken != null,
            "hasRefreshToken", refreshToken != null,
            "accessTokenValid", hasValidAccessToken,
            "refreshTokenValid", hasValidRefreshToken && refreshTokenInDb,
            "authenticated", hasValidAccessToken || (hasValidRefreshToken && refreshTokenInDb)
        ));
    }

    private void addTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}

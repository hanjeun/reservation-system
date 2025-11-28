package ac.inhatc.reservation_system.config.controller;

import ac.inhatc.reservation_system.config.jwt.JwtProperties;
import ac.inhatc.reservation_system.config.jwt.TokenProvider;
import ac.inhatc.reservation_system.config.jwt.repository.RefreshTokenRepository;
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

            log.info("🔐 로그인 시도: email={}", email);

            // 사용자 찾기
            Member member = memberRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

            log.info("👤 사용자 찾음: id={}, name={}", member.getId(), member.getName());

            // 비밀번호 확인
            if (!passwordEncoder.matches(password, member.getPassword())) {
                log.warn("❌ 비밀번호 불일치: email={}", email);
                return ResponseEntity.status(401).body(Map.of("error", "비밀번호가 일치하지 않습니다."));
            }

            log.info("✅ 비밀번호 일치 확인");

            // JWT 토큰 생성 및 쿠키 저장 (generateRefreshToken이 DB에 자동 저장)
            String accessToken = tokenProvider.generateAccessToken(member);
            String refreshToken = tokenProvider.generateRefreshToken(member);

            log.info("🎫 JWT 토큰 생성 완료");

            addTokenCookie(response, "access_token", accessToken,
                (int) jwtProperties.getAccessTokenExpiration().toSeconds());
            addTokenCookie(response, "refresh_token", refreshToken,
                (int) jwtProperties.getRefreshTokenExpiration().toSeconds());

            log.info("✅ 로그인 성공: email={}, userId={}", email, member.getId());

            return ResponseEntity.ok(Map.of(
                "success", true,
                "user", Map.of(
                    "id", member.getId(),
                    "name", member.getName(),
                    "email", member.getEmail()
                )
            ));
        } catch (IllegalArgumentException e) {
            log.error("❌ 로그인 실패: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ 로그인 중 예상치 못한 오류", e);
            return ResponseEntity.status(500).body(Map.of("error", "로그인 처리 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            // refresh_token 쿠키에서 토큰 가져오기
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("refresh_token".equals(cookie.getName())) {
                        String refreshToken = cookie.getValue();
                        // DB에서 refresh token 삭제
                        refreshTokenRepository.findByRefreshToken(refreshToken)
                                .ifPresent(token -> {
                                    refreshTokenRepository.delete(token);
                                    log.info("🗑️ Refresh Token 삭제됨: userId={}", token.getUserId());
                                });
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ 로그아웃 중 토큰 삭제 실패 (무시 가능): {}", e.getMessage());
        }

        // 쿠키 삭제
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

        log.info("✅ 로그아웃 완료");
        return ResponseEntity.ok(Map.of("success", true));
    }

    /**
     * 현재 로그인 상태 확인 (디버깅용)
     */
    @PostMapping("/check")
    public ResponseEntity<?> checkAuth(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        boolean hasAccessToken = false;
        boolean hasRefreshToken = false;

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    hasAccessToken = true;
                    log.info("🍪 Access Token 쿠키 존재: {}", cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "...");
                } else if ("refresh_token".equals(cookie.getName())) {
                    hasRefreshToken = true;
                    log.info("🍪 Refresh Token 쿠키 존재");
                }
            }
        }

        return ResponseEntity.ok(Map.of(
            "hasAccessToken", hasAccessToken,
            "hasRefreshToken", hasRefreshToken,
            "authenticated", hasAccessToken && hasRefreshToken
        ));
    }

    private void addTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true); // XSS 공격 방지 - JavaScript에서 접근 불가
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        // cookie.setSecure(true); // HTTPS 환경에서만 전송 (프로덕션에서 활성화 권장)
        response.addCookie(cookie);
    }
}

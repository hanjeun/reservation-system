package ac.inhatc.reservation_system.config.oauth2;

import ac.inhatc.reservation_system.config.jwt.JwtProperties;
import ac.inhatc.reservation_system.config.jwt.TokenProvider;
import ac.inhatc.reservation_system.member.entity.Member;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 로그인 성공 시 JWT 토큰을 발급하고 리다이렉트하는 핸들러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        Member member = oAuth2User.getMember();

        log.info("✅ OAuth2 로그인 성공 - Email: {}, Provider: {}",
                member.getEmail(), member.getProvider());

        // 1. Access Token 생성
        String accessToken = tokenProvider.generateAccessToken(member);

        // 2. Refresh Token 생성 및 DB 저장
        String refreshToken = tokenProvider.generateRefreshToken(member);

        log.info("🎫 JWT 토큰 발급 완료 - Access Token 길이: {}", accessToken.length());

        // 3. JWT 쿠키 저장
        addTokenCookie(response, "access_token", accessToken,
                (int) jwtProperties.getAccessTokenExpiration().toSeconds());
        addTokenCookie(response, "refresh_token", refreshToken,
                (int) jwtProperties.getRefreshTokenExpiration().toSeconds());

        log.info("🍪 JWT 쿠키 저장 완료");

        // 4. OAuth2 로그인에 사용된 세션 무효화 + JSESSIONID 쿠키 삭제
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
            log.info("🗑️ OAuth2 세션 무효화 완료");
        }
        
        // JSESSIONID 쿠키 삭제
        Cookie jsessionCookie = new Cookie("JSESSIONID", "");
        jsessionCookie.setPath("/");
        jsessionCookie.setMaxAge(0);
        response.addCookie(jsessionCookie);
        log.info("🗑️ JSESSIONID 쿠키 삭제 완료");

        // 5. 메인 페이지로 리다이렉트
        String targetUrl = determineTargetUrl(request, response, authentication);
        log.info("🔀 리다이렉트: {}", targetUrl);

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * 쿠키에 토큰 저장
     */
    private void addTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(maxAge);
        cookie.setSecure(false);  // 개발환경: false, 프로덕션(HTTPS): true
        response.addCookie(cookie);
    }

    /**
     * 리다이렉트할 URL 결정
     */
    @Override
    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        return "/";  // 항상 메인 페이지로 리다이렉트
    }
}

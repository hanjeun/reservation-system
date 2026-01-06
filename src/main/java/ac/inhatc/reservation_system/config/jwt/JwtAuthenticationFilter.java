package ac.inhatc.reservation_system.config.jwt;

import ac.inhatc.reservation_system.member.entity.Member;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * JWT 인증 필터
 * 모든 HTTP 요청에서 JWT 토큰을 확인하고 검증합니다.
 * 
 * 토큰 추출 우선순위:
 * 1. Authorization 헤더 (앱, API 클라이언트용)
 * 2. 쿠키 (웹 브라우저용)
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements Filter {

    private final TokenProvider tokenProvider;
    private static final String COOKIE_NAME = "access_token";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 공개 URL인 경우 필터를 건너뜀
        String requestURI = httpRequest.getRequestURI();
        if (isPublicUrl(requestURI)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 토큰 추출 (헤더 우선 → 쿠키)
            String token = resolveToken(httpRequest);

            // 토큰이 있고 유효한 경우
            if (token != null && tokenProvider.validToken(token)) {
                // 토큰에서 사용자 정보 추출
                Member member = tokenProvider.getMemberFromToken(token);
                
                // 실제 사용자 역할로 권한 설정
                String role = "ROLE_" + member.getRole().name();
                
                // SecurityContext에 인증 정보 설정
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    member, 
                    token, 
                    Collections.singletonList(new SimpleGrantedAuthority(role))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // request에도 사용자 정보 저장 (컨트롤러에서 사용 가능)
                httpRequest.setAttribute("authenticatedUser", member);
                
                log.debug("✅ JWT 인증 성공: email={}, role={}", member.getEmail(), role);
            } else {
                // 토큰이 없거나 유효하지 않은 경우
                SecurityContextHolder.clearContext();
            }
            
            // 다음 필터로 진행
            chain.doFilter(request, response);
            
        } catch (Exception e) {
            // 인증 실패 시 SecurityContext 초기화
            SecurityContextHolder.clearContext();
            log.error("❌ JWT 인증 필터 에러: {}", e.getMessage());
            chain.doFilter(request, response);
        }
    }

    /**
     * 토큰 추출 (헤더 우선 → 쿠키)
     * 
     * 앱/API: Authorization: Bearer {token}
     * 웹: Cookie: access_token={token}
     */
    private String resolveToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 추출 (앱, API 클라이언트)
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            String token = bearerToken.substring(BEARER_PREFIX.length());
            log.debug("🔑 Authorization 헤더에서 토큰 추출");
            return token;
        }

        // 2. 쿠키에서 추출 (웹 브라우저)
        String cookieToken = getTokenFromCookie(request);
        if (cookieToken != null) {
            log.debug("🍪 쿠키에서 토큰 추출");
            return cookieToken;
        }

        return null;
    }

    /**
     * 쿠키에서 access_token 추출
     */
    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            return Arrays.stream(cookies)
                    .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        return null;
    }

    /**
     * 공개 URL인지 확인
     */
    private boolean isPublicUrl(String requestURI) {
        return requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/images/") ||
               requestURI.startsWith("/uploads/") ||
               requestURI.startsWith("/vendor/") ||
               requestURI.startsWith("/oauth2/") ||
               requestURI.startsWith("/login/oauth2/") ||
               requestURI.equals("/favicon.ico") ||
               requestURI.equals("/favicon.svg") ||
               requestURI.equals("/") ||
               requestURI.equals("/main") ||
               requestURI.equals("/user/login") ||
               requestURI.equals("/user/signup") ||
               requestURI.equals("/customer-service/policy") ||
               requestURI.startsWith("/api/auth/") ||
               requestURI.startsWith("/api/email/") ||
               requestURI.equals("/api/token") ||
               requestURI.equals("/hc") ||
               requestURI.equals("/env");
    }
}

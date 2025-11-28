package ac.inhatc.reservation_system.config.jwt;

import ac.inhatc.reservation_system.member.entity.Member;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 * JWT 인증 필터
 * 모든 HTTP 요청에서 쿠키의 access_token을 확인하고 검증합니다.
 * 유효한 토큰이 있으면 SecurityContext에 인증 정보를 설정합니다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements Filter {

    private final TokenProvider tokenProvider;
    private static final String COOKIE_NAME = "access_token";

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
            // 쿠키에서 토큰 추출
            String token = getTokenFromCookie(httpRequest);

            // 토큰이 있고 유효한 경우
            if (token != null && tokenProvider.validToken(token)) {
                // 토큰에서 사용자 정보 추출
                Member member = tokenProvider.getMemberFromToken(token);
                
                // SecurityContext에 인증 정보 설정
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    member, 
                    token, 
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // request에도 사용자 정보 저장 (컨트롤러에서 사용 가능)
                httpRequest.setAttribute("authenticatedUser", member);
                
                // 다음 필터로 진행
                chain.doFilter(request, response);
            } else {
                // 토큰이 없거나 유효하지 않은 경우
                SecurityContextHolder.clearContext();
                
                // 다음 필터로 진행 (Spring Security가 처리)
                chain.doFilter(request, response);
            }
            
        } catch (Exception e) {
            // 인증 실패 시 SecurityContext 초기화
            SecurityContextHolder.clearContext();
            
            // 에러 로깅
            System.err.println("JWT 인증 필터 에러: " + e.getMessage());
            e.printStackTrace();
            
            // 다음 필터로 진행 (Spring Security가 처리)
            chain.doFilter(request, response);
        }
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
     * 이 URL들은 JWT 검증 없이 접근 가능
     */
    private boolean isPublicUrl(String requestURI) {
        return requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/images/") ||
               requestURI.startsWith("/uploads/") ||
               requestURI.equals("/") ||
               requestURI.equals("/main") ||
               requestURI.equals("/user/login") ||
               requestURI.equals("/user/signup") ||
               requestURI.startsWith("/api/auth/") ||
               requestURI.equals("/api/token");
    }
}

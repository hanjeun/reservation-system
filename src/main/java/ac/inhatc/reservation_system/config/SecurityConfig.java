package ac.inhatc.reservation_system.config;

import ac.inhatc.reservation_system.config.jwt.JwtAuthenticationFilter;
import ac.inhatc.reservation_system.config.jwt.TokenProvider;
import ac.inhatc.reservation_system.config.oauth2.CustomOAuth2UserService;
import ac.inhatc.reservation_system.config.oauth2.HttpCookieOAuth2AuthorizationRequestRepository;
import ac.inhatc.reservation_system.config.oauth2.OAuth2AuthenticationFailureHandler;
import ac.inhatc.reservation_system.config.oauth2.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TokenProvider tokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // 1. CSRF 비활성화 (JWT 사용 시 불필요)
                .csrf(csrf -> csrf.disable())
                
                // 2. HTTP Basic 인증 비활성화
                .httpBasic(httpBasic -> httpBasic.disable())
                
                // 3. 세션 설정 - 완전 STATELESS (세션 생성 안함)
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 4. 폼 로그인 비활성화
                .formLogin(form -> form.disable())
                
                // 5. 로그아웃 비활성화
                .logout(logout -> logout.disable())
                
                // 6. URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스 (누구나 접근 가능)
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/uploads/**", "/vendor/**", "/favicon.svg").permitAll()
                        
                        // 공개 페이지 (누구나 접근 가능)
                        .requestMatchers("/", "/main", "/user/login", "/user/signup").permitAll()
                        
                        // OAuth2 로그인 관련 (누구나 접근 가능)
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        
                        // 고객센터 - 정책 페이지만 공개
                        .requestMatchers("/customer-service/policy").permitAll()
                        
                        // 인증 API (누구나 접근 가능)
                        .requestMatchers("/api/auth/**").permitAll()
                        
                        // 이메일 인증 API (누구나 접근 가능)
                        .requestMatchers("/api/email/**").permitAll()
                        
                        // API 토큰 발급 (누구나 접근 가능)
                        .requestMatchers("/api/token").permitAll()

                        // Health Check (누구나 접근 가능)
                        .requestMatchers("/hc", "/env").permitAll()
                        
                        // 관리자 전용
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/business-verification/admin/**").hasRole("ADMIN")
                        
                        // 사업자 인증 API
                        .requestMatchers("/api/business-verification/submit", "/api/business-verification/my-status", "/api/business-verification/cancel", "/api/business-verification/resign").authenticated()
                        
                        // 홍보/추천 페이지
                        .requestMatchers("/store/promotion").authenticated()
                        
                        // 가게 관련
                        .requestMatchers("/store/**").authenticated()
                        
                        // API
                        .requestMatchers("/api/**").authenticated()
                        
                        // 기타 모든 요청
                        .anyRequest().authenticated())
                
                // 7. OAuth2 로그인 설정 - 쿠키 기반 Authorization Request Repository 사용
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/user/login")
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository(httpCookieOAuth2AuthorizationRequestRepository))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler))
                
                // 8. JWT 인증 필터 등록
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                
                // 9. 인증 실패 시 예외 처리
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            String requestURI = request.getRequestURI();
                            if (requestURI.startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"error\":\"인증이 필요합니다\",\"message\":\"" 
                                    + authException.getMessage() + "\"}");
                            } else {
                                response.sendRedirect("/user/login");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String requestURI = request.getRequestURI();
                            if (requestURI.startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"error\":\"접근이 거부되었습니다\",\"message\":\"" 
                                    + accessDeniedException.getMessage() + "\"}");
                            } else {
                                response.sendRedirect("/user/login");
                            }
                        }))
                
                .build();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider);
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

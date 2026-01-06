package ac.inhatc.reservation_system.config;

import ac.inhatc.reservation_system.config.jwt.JwtAuthenticationFilter;
import ac.inhatc.reservation_system.config.jwt.TokenProvider;
import ac.inhatc.reservation_system.config.oauth2.CustomOAuth2UserService;
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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // 1. CSRF 비활성화 (JWT 사용 시 불필요)
                .csrf(csrf -> csrf.disable())
                
                // 2. HTTP Basic 인증 비활성화
                .httpBasic(httpBasic -> httpBasic.disable())
                
                // 3. 세션 설정 - OAuth2 로그인에만 세션 사용 (로그인 후 즉시 삭제)
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                
                // 4. 폼 로그인 비활성화
                .formLogin(form -> form.disable())
                
                // 5. 로그아웃
                .logout(logout -> logout
                        // 프론트엔드 handleLogout()에서 호출하는 URL과 일치하는지 확인해야 합니다.
                        .logoutUrl("/api/auth/logout")

                        // 로그아웃 성공 시 이동할 경로 (예: 메인 페이지)
                        .logoutSuccessUrl("/main")

                        // 서버 세션 무효화
                        .invalidateHttpSession(true)

                        // ⭐⭐ JSESSIONID 쿠키 명시적 제거 ⭐⭐
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                
                // 6. URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/uploads/**", "/vendor/**", "/favicon.svg").permitAll()
                        
                        // 공개 페이지
                        .requestMatchers("/", "/main", "/user/login", "/user/signup").permitAll()
                        
                        // OAuth2 로그인 관련
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        
                        // 고객센터 정책 페이지
                        .requestMatchers("/customer-service/policy").permitAll()
                        
                        // 인증 API
                        .requestMatchers("/api/auth/**").permitAll()
                        
                        // 이메일 인증 API
                        .requestMatchers("/api/email/**").permitAll()
                        
                        // API 토큰 발급
                        .requestMatchers("/api/token").permitAll()

                        // Health Check
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
                
                // 7. OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/user/login")
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

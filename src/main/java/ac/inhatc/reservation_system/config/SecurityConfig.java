package ac.inhatc.reservation_system.config;

import ac.inhatc.reservation_system.config.jwt.JwtAuthenticationFilter;
import ac.inhatc.reservation_system.config.jwt.TokenProvider;
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
import org.springframework.security.web.context.NullSecurityContextRepository;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TokenProvider tokenProvider;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // 1. CSRF 비활성화 (JWT 사용 시 불필요)
                .csrf(csrf -> csrf.disable())
                
                // 2. HTTP Basic 인증 비활성화
                .httpBasic(httpBasic -> httpBasic.disable())
                
                // 3. 세션 완전 비활성화 (STATELESS) - JWT의 핵심!
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // 4. SecurityContext를 세션에 저장하지 않음
                .securityContext(context -> context
                    .securityContextRepository(new NullSecurityContextRepository()))
                
                // 5. Request Cache 비활성화 (세션 사용 방지)
                .requestCache(cache -> cache.disable())
                
                // 6. 폼 로그인 비활성화 (세션 생성 방지)
                .formLogin(form -> form.disable())
                
                // 7. 로그아웃 비활성화 (세션 기반 로그아웃 방지)
                .logout(logout -> logout.disable())
                
                // 8. URL별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스 (누구나 접근 가능)
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**", "/uploads/**", "/vendor/**", "/favicon.svg").permitAll()
                        
                        // 공개 페이지 (누구나 접근 가능)
                        .requestMatchers("/", "/main", "/user/login", "/user/signup").permitAll()
                        
                        // 고객센터 - 정책 페이지만 공개 (공지사항, 문의하기는 로그인 필요)
                        .requestMatchers("/customer-service/policy").permitAll()
                        
                        // 인증 API (누구나 접근 가능) - 로그인/로그아웃
                        .requestMatchers("/api/auth/**").permitAll()
                        
                        // API 토큰 발급 (누구나 접근 가능)
                        .requestMatchers("/api/token").permitAll()
                        
                        // 홍보/추천 페이지 (인증 필요 - 일반 사용자는 "추천 가게", 사업자는 "홍보하기")
                        .requestMatchers("/store/promotion").authenticated()
                        
                        // 가게 관련 (JWT 인증 필요)
                        .requestMatchers("/store/**").authenticated()
                        
                        // API (JWT 인증 필요)
                        .requestMatchers("/api/**").authenticated()
                        
                        // 기타 모든 요청 (JWT 인증 필요)
                        .anyRequest().authenticated())
                
                // 9. JWT 인증 필터 등록 (가장 중요!)
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                
                // 10. 인증 실패 시 예외 처리
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            // API 요청인 경우 JSON 응답
                            String requestURI = request.getRequestURI();
                            if (requestURI.startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"error\":\"인증이 필요합니다\",\"message\":\"" 
                                    + authException.getMessage() + "\"}");
                            } else {
                                // 일반 페이지 요청인 경우 로그인 페이지로 리다이렉트
                                response.sendRedirect("/user/login");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // API 요청인 경우 JSON 응답
                            String requestURI = request.getRequestURI();
                            if (requestURI.startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"error\":\"접근이 거부되었습니다\",\"message\":\"" 
                                    + accessDeniedException.getMessage() + "\"}");
                            } else {
                                // 일반 페이지 요청인 경우 로그인 페이지로 리다이렉트
                                response.sendRedirect("/user/login");
                            }
                        }))
                
                .build();
    }

    /**
     * JWT 인증 필터 Bean 등록
     * 모든 요청에서 쿠키의 access_token을 검증합니다
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(tokenProvider);
    }

    /**
     * 비밀번호 암호화를 위한 Encoder
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

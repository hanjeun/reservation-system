package ac.inhatc.reservation_system.config.handler;

// ❌❌❌ 이 파일은 완전히 사용되지 않습니다 ❌❌❌
// 
// 이전 방식: formLogin + Spring Security의 AuthenticationSuccessHandler
// 현재 방식: JWT + AuthApiController (REST API)
//
// SecurityConfig에서 formLogin()을 제거했기 때문에
// 이 핸들러는 더 이상 호출되지 않습니다.
//
// JWT 로그인은 /api/auth/login (AuthApiController)에서 처리합니다.
//
// 정리 이유:
// 1. SecurityConfig에서 formLogin 제거됨
// 2. JWT는 REST API 방식으로 로그인 처리
// 3. 이 핸들러는 호출될 일이 없음
//
// 이 파일은 안전하게 삭제할 수 있습니다.

/*
import ac.inhatc.reservation_system.config.jwt.JwtProperties;
import ac.inhatc.reservation_system.config.jwt.TokenProvider;
import ac.inhatc.reservation_system.config.jwt.entity.RefreshToken;
import ac.inhatc.reservation_system.config.jwt.repository.RefreshTokenRepository;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.repository.MemberRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    // ... 코드 생략 ...
}
*/

package ac.inhatc.reservation_system.config.service;

import ac.inhatc.reservation_system.config.jwt.TokenProvider;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class TokenService {

    private final TokenProvider tokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final MemberService memberService;

    public String createNewAccessToken(String refreshToken) throws IllegalAccessException {
        log.info("========== 🔄 Access Token 재발급 시작 ==========");
        log.info("[1단계] Refresh Token 검증 중... Token: {}...", refreshToken.substring(0, Math.min(20, refreshToken.length())));
        
        if (!tokenProvider.validToken(refreshToken)) {
            log.error("❌ Refresh Token 검증 실패: 유효하지 않은 토큰");
            throw new IllegalAccessException("Unexpected token");
        }
        log.info("✅ Refresh Token 검증 성공");

        log.info("[2단계] Refresh Token으로 사용자 정보 조회 중...");
        Long userId = refreshTokenService.findByRefreshToken(refreshToken).getUserId();
        Member member = memberService.findById(userId);
        log.info("✅ 사용자 정보 조회 완료 - User ID: {}, Email: {}, Role: {}", userId, member.getEmail(), member.getRole());

        log.info("[3단계] 새로운 Access Token 생성 중...");
        String newAccessToken = tokenProvider.generateAccessToken(member);
        log.info("✅ 새로운 Access Token 생성 완료: {}...", newAccessToken.substring(0, Math.min(20, newAccessToken.length())));
        log.info("========== ✅ Access Token 재발급 완료 ==========\n");
        
        return newAccessToken;
    }
}

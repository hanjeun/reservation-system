package ac.inhatc.reservation_system.config.service;

import ac.inhatc.reservation_system.config.jwt.JwtProperties;
import ac.inhatc.reservation_system.config.jwt.TokenProvider;
import ac.inhatc.reservation_system.config.jwt.entity.RefreshToken;
import ac.inhatc.reservation_system.config.jwt.repository.RefreshTokenRepository;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.entity.Role;
import ac.inhatc.reservation_system.member.repository.MemberRepository;
import ac.inhatc.reservation_system.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JWT Refresh Token을 이용한 Access Token 재발급 테스트
 * 
 * 이 테스트는 다음을 검증합니다:
 * 1. Refresh Token이 유효한 경우 Access Token 재발급 성공
 * 2. Refresh Token이 만료된 경우 재발급 실패
 * 3. 존재하지 않는 Refresh Token으로 재발급 시도 시 실패
 * 4. 재발급된 Access Token이 정상적으로 검증되는지 확인
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class TokenServiceTest {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private JwtProperties jwtProperties;

    private Member testMember;
    private String validRefreshToken;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성
        testMember = Member.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .name("테스트유저")
                .role(Role.USER)
                .build();
        testMember = memberRepository.save(testMember);

        // 유효한 Refresh Token 생성
        validRefreshToken = tokenProvider.generateRefreshToken(testMember);

        // DB에 Refresh Token 저장
        RefreshToken refreshToken = new RefreshToken(testMember.getId(), validRefreshToken);
        refreshTokenRepository.save(refreshToken);

        System.out.println("\n========== 테스트 설정 완료 ==========");
        System.out.println("테스트 회원 ID: " + testMember.getId());
        System.out.println("테스트 회원 이메일: " + testMember.getEmail());
        System.out.println("Refresh Token 생성 완료");
        System.out.println("Refresh Token 만료 시간: " + jwtProperties.getRefreshTokenExpiration().toDays() + "일");
        System.out.println("=====================================\n");
    }

    @Test
    @DisplayName("유효한 Refresh Token으로 Access Token 재발급 성공")
    void createNewAccessToken_WithValidRefreshToken_Success() throws IllegalAccessException {
        // given
        System.out.println("\n========== 테스트 시작: 유효한 Refresh Token으로 Access Token 재발급 ==========");
        System.out.println("사용할 Refresh Token: " + validRefreshToken.substring(0, 20) + "...");

        // when
        System.out.println("\n[1단계] Refresh Token으로 새로운 Access Token 생성 중...");
        String newAccessToken = tokenService.createNewAccessToken(validRefreshToken);
        System.out.println("✅ 새로운 Access Token 생성 완료: " + newAccessToken.substring(0, 20) + "...");

        // then
        System.out.println("\n[2단계] 생성된 Access Token 검증 중...");
        assertThat(newAccessToken).isNotNull();
        assertThat(tokenProvider.validToken(newAccessToken)).isTrue();
        System.out.println("✅ Access Token 유효성 검증 완료");

        System.out.println("\n[3단계] Access Token에서 사용자 정보 추출 중...");
        Long userId = tokenProvider.getUserId(newAccessToken);
        assertThat(userId).isEqualTo(testMember.getId());
        System.out.println("✅ 추출된 User ID: " + userId);
        System.out.println("✅ 예상 User ID: " + testMember.getId());
        System.out.println("✅ User ID 일치 확인 완료");

        System.out.println("\n[4단계] Access Token으로 회원 정보 조회 중...");
        Member memberFromToken = tokenProvider.getMemberFromToken(newAccessToken);
        assertThat(memberFromToken.getId()).isEqualTo(testMember.getId());
        assertThat(memberFromToken.getEmail()).isEqualTo(testMember.getEmail());
        System.out.println("✅ 조회된 회원 이메일: " + memberFromToken.getEmail());
        System.out.println("✅ 회원 정보 일치 확인 완료");

        System.out.println("\n========== ✅ 테스트 성공: Refresh Token을 이용한 Access Token 재발급 완료 ==========\n");
    }

    @Test
    @DisplayName("존재하지 않는 Refresh Token으로 재발급 시도 시 실패")
    void createNewAccessToken_WithNonExistentRefreshToken_ThrowsException() {
        // given
        System.out.println("\n========== 테스트 시작: 존재하지 않는 Refresh Token으로 재발급 시도 ==========");
        String nonExistentRefreshToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.token";
        System.out.println("존재하지 않는 Refresh Token: " + nonExistentRefreshToken.substring(0, 20) + "...");

        // when & then
        System.out.println("\n[검증] 재발급 시도 중... IllegalArgumentException 예상");
        assertThatThrownBy(() -> tokenService.createNewAccessToken(nonExistentRefreshToken))
                .isInstanceOf(java.lang.IllegalAccessException.class)
                .hasMessage("Unexpected token");
        System.out.println("✅ 예상대로 IllegalArgumentException 발생 확인");

        System.out.println("\n========== ✅ 테스트 성공: 잘못된 토큰에 대한 예외 처리 확인 완료 ==========\n");
    }

    @Test
    @DisplayName("만료된 Refresh Token으로 재발급 시도 시 실패")
    void createNewAccessToken_WithExpiredRefreshToken_ThrowsException() {
        // given
        System.out.println("\n========== 테스트 시작: 만료된 Refresh Token으로 재발급 시도 ==========");
        
        // 만료된 토큰 생성 (만료 시간을 0으로 설정)
        System.out.println("[1단계] 만료된 Refresh Token 생성 중...");
        String expiredToken = tokenProvider.generateToken(testMember, Duration.ZERO);
        System.out.println("만료된 Refresh Token 생성 완료");

        // 기존 토큰 업데이트 (새로 저장하지 않고 업데이트)
        RefreshToken existingToken = refreshTokenRepository.findByRefreshToken(validRefreshToken)
                .orElseThrow();
        existingToken.update(expiredToken);
        refreshTokenRepository.save(existingToken);
        System.out.println("DB에 만료된 토큰으로 업데이트 완료");

        // 토큰 만료 확인
        System.out.println("\n[2단계] 토큰 만료 여부 확인 중...");
        boolean isValid = tokenProvider.validToken(expiredToken);
        System.out.println("토큰 유효성: " + isValid);
        assertThat(isValid).isFalse();
        System.out.println("✅ 토큰이 만료된 것을 확인");

        // when & then
        System.out.println("\n[3단계] 만료된 토큰으로 재발급 시도... IllegalAccessException 예상");
        assertThatThrownBy(() -> tokenService.createNewAccessToken(expiredToken))
                .isInstanceOf(IllegalAccessException.class)
                .hasMessage("Unexpected token");
        System.out.println("✅ 예상대로 IllegalAccessException 발생 확인");

        System.out.println("\n========== ✅ 테스트 성공: 만료된 토큰에 대한 예외 처리 확인 완료 ==========\n");
    }

    @Test
    @DisplayName("여러 번 Access Token 재발급 가능 (Refresh Token 재사용)")
    void createNewAccessToken_MultipleTimesWithSameRefreshToken_Success() throws IllegalAccessException {
        // given
        System.out.println("\n========== 테스트 시작: Refresh Token으로 여러 번 Access Token 재발급 ==========");
        System.out.println("동일한 Refresh Token으로 3번 재발급 시도");

        // when & then
        for (int i = 1; i <= 3; i++) {
            System.out.println("\n[" + i + "번째 재발급] Refresh Token으로 Access Token 생성 중...");
            String accessToken = tokenService.createNewAccessToken(validRefreshToken);
            
            System.out.println("생성된 Access Token: " + accessToken.substring(0, 20) + "...");
            assertThat(accessToken).isNotNull();
            assertThat(tokenProvider.validToken(accessToken)).isTrue();
            System.out.println("✅ " + i + "번째 Access Token 생성 및 검증 완료");

            // 각 토큰으로 사용자 정보 확인
            Long userId = tokenProvider.getUserId(accessToken);
            assertThat(userId).isEqualTo(testMember.getId());
            System.out.println("✅ User ID 확인: " + userId);
        }

        System.out.println("\n========== ✅ 테스트 성공: 동일한 Refresh Token으로 여러 번 재발급 가능 확인 ==========\n");
    }

    @Test
    @DisplayName("재발급된 Access Token이 사용자의 Role 정보를 포함하는지 확인")
    void createNewAccessToken_ContainsUserRole() throws IllegalAccessException {
        // given
        System.out.println("\n========== 테스트 시작: 재발급된 Access Token의 Role 정보 확인 ==========");
        System.out.println("테스트 회원 Role: " + testMember.getRole());

        // when
        System.out.println("\n[1단계] Access Token 재발급 중...");
        String newAccessToken = tokenService.createNewAccessToken(validRefreshToken);
        System.out.println("✅ Access Token 생성 완료");

        // then
        System.out.println("\n[2단계] Access Token에서 회원 정보 추출 중...");
        Member memberFromToken = tokenProvider.getMemberFromToken(newAccessToken);
        
        assertThat(memberFromToken.getRole()).isEqualTo(testMember.getRole());
        System.out.println("✅ Token의 Role: " + memberFromToken.getRole());
        System.out.println("✅ 원본 회원 Role: " + testMember.getRole());
        System.out.println("✅ Role 정보 일치 확인 완료");

        System.out.println("\n========== ✅ 테스트 성공: Access Token이 올바른 Role 정보를 포함함 ==========\n");
    }
}

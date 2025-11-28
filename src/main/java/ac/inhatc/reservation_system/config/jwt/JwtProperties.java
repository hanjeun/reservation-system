package ac.inhatc.reservation_system.config.jwt;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties("jwt")
public class JwtProperties {

    private String issuer;
    private String secretKey;
    private TokenConfig accessToken = new TokenConfig();
    private TokenConfig refreshToken = new TokenConfig();

    @PostConstruct
    public void validate() {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT secret key가 설정되지 않았습니다. " +
                "시스템 환경변수 'SecretKey'를 확인하세요."
            );
        }
        
        // 최소 길이 검증 (256비트 = 32바이트 = 64자리 hex)
        if (secretKey.length() < 64) {
            throw new IllegalStateException(
                "JWT secret key는 최소 64자 이상이어야 합니다. " +
                "현재 길이: " + secretKey.length()
            );
        }
        
        System.out.println("✅ JWT 설정 로드 완료");
        System.out.println("  - SecretKey 길이: " + secretKey.length());
        System.out.println("  - Access Token 만료시간: " + accessToken.getExpirationMinutes() + "분");
        System.out.println("  - Refresh Token 만료시간: " + refreshToken.getExpirationDays() + "일");
    }

    /**
     * 토큰 설정을 담는 내부 클래스
     */
    @Getter
    @Setter
    public static class TokenConfig {
        private Integer expirationMinutes;  // Access Token용 (분 단위)
        private Integer expirationDays;     // Refresh Token용 (일 단위)

        /**
         * 만료 시간을 Duration으로 반환 (분 단위)
         */
        public Duration getExpirationDuration() {
            if (expirationMinutes != null) {
                return Duration.ofMinutes(expirationMinutes);
            }
            if (expirationDays != null) {
                return Duration.ofDays(expirationDays);
            }
            throw new IllegalStateException("토큰 만료시간이 설정되지 않았습니다.");
        }
    }

    /**
     * Access Token 만료시간 반환
     */
    public Duration getAccessTokenExpiration() {
        return accessToken.getExpirationDuration();
    }

    /**
     * Refresh Token 만료시간 반환
     */
    public Duration getRefreshTokenExpiration() {
        return refreshToken.getExpirationDuration();
    }
}

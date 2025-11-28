package ac.inhatc.reservation_system.config.jwt;

import ac.inhatc.reservation_system.config.jwt.entity.RefreshToken;
import ac.inhatc.reservation_system.config.jwt.repository.RefreshTokenRepository;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class TokenProvider {

    private final JwtProperties jwtProperties;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private SecretKey cachedSecretKey;

    @PostConstruct
    public void init() {
        this.cachedSecretKey = Keys.hmacShaKeyFor(
                jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(Member member) {
        return generateToken(member, jwtProperties.getAccessTokenExpiration());
    }

    public String generateRefreshToken(Member member) {
        String token = generateToken(member, jwtProperties.getRefreshTokenExpiration());
        
        // 기존 토큰 모두 삭제 (단일 기기 로그인 방식)
        List<RefreshToken> existingTokens = refreshTokenRepository.findByUserId(member.getId());
        if (!existingTokens.isEmpty()) {
            log.info("🗑️ 기존 Refresh Token 삭제: userId={}, count={}", member.getId(), existingTokens.size());
            refreshTokenRepository.deleteAll(existingTokens);
        }
        
        // 새로운 refresh token 저장
        RefreshToken refreshToken = new RefreshToken(member.getId(), token);
        refreshTokenRepository.save(refreshToken);
        log.info("💾 새로운 Refresh Token 저장: userId={}", member.getId());
        
        return token;
    }


    public String generateToken(Member member, Duration expiredAt) {
        Date now = new Date();
        return makeToken(member, new Date(now.getTime() + expiredAt.toMillis()));
    }

    private String makeToken(Member member, Date expiry) {
        Date now = new Date();
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setIssuer(jwtProperties.getIssuer())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setSubject(member.getEmail())
                .claim("id", member.getId())
                .claim("role", member.getRole().name()) // 권한 정보 추가
                .signWith(cachedSecretKey)
                .compact();
    }

    public boolean validToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(cachedSecretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        return claims.get("id", Long.class);
    }

    public Member getMemberFromToken(String token) {
        Long userId = getUserId(token);
        return memberRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(cachedSecretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}

package ac.inhatc.reservation_system.config.jwt.repository;

import ac.inhatc.reservation_system.config.jwt.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    // 사용자의 모든 refresh token 조회
    List<RefreshToken> findByUserId(Long userId);
    
    // 특정 refresh token 조회
    Optional<RefreshToken> findByRefreshToken(String refreshToken);
}

package ac.inhatc.reservation_system.member.repository;

import ac.inhatc.reservation_system.member.entity.AuthProvider;
import ac.inhatc.reservation_system.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository <Member, Long>{
    Optional<Member> findByEmail(String email);
    
    // OAuth2 로그인용: provider와 providerId로 회원 조회
    Optional<Member> findByProviderAndProviderId(AuthProvider provider, String providerId);
}

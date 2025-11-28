package ac.inhatc.reservation_system.member.repository;

import ac.inhatc.reservation_system.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository <Member, Long>{
    Optional<Member> findByEmail(String email);
}

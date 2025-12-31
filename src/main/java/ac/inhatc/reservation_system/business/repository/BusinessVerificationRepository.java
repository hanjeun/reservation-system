package ac.inhatc.reservation_system.business.repository;

import ac.inhatc.reservation_system.business.entity.BusinessVerification;
import ac.inhatc.reservation_system.business.entity.BusinessVerification.VerificationStatus;
import ac.inhatc.reservation_system.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusinessVerificationRepository extends JpaRepository<BusinessVerification, Long> {

    // 회원의 최신 인증 요청 조회
    Optional<BusinessVerification> findTopByMemberOrderByCreatedAtDesc(Member member);

    // 회원 ID로 최신 인증 요청 조회
    Optional<BusinessVerification> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);

    // 회원의 대기중인 인증 요청 조회
    Optional<BusinessVerification> findByMemberAndStatus(Member member, VerificationStatus status);

    // 회원의 특정 상태 인증 요청 조회 (최신)
    Optional<BusinessVerification> findTopByMemberAndStatusOrderByCreatedAtDesc(Member member, VerificationStatus status);

    // 상태별 인증 요청 목록 조회 (페이징)
    Page<BusinessVerification> findByStatusOrderByCreatedAtDesc(VerificationStatus status, Pageable pageable);

    // 전체 인증 요청 목록 조회 (최신순)
    Page<BusinessVerification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 대기중인 인증 요청 수
    long countByStatus(VerificationStatus status);

    // 회원이 이미 대기중인 요청이 있는지 확인
    boolean existsByMemberAndStatus(Member member, VerificationStatus status);

    // 회원 ID로 대기중인 요청이 있는지 확인
    boolean existsByMemberIdAndStatus(Long memberId, VerificationStatus status);

    // 회원의 모든 인증 요청 조회
    List<BusinessVerification> findByMemberOrderByCreatedAtDesc(Member member);

    // 회원 삭제 시 관련 인증 요청 삭제
    @Modifying
    @Query("DELETE FROM BusinessVerification bv WHERE bv.member.id = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}

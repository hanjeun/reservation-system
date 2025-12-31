package ac.inhatc.reservation_system.customerservice.repository;

import ac.inhatc.reservation_system.customerservice.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // 회원별 문의 조회 (페이징)
    Page<Inquiry> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    // 전체 문의 조회 (관리자용, 페이징)
    Page<Inquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    // 카테고리별 조회
    Page<Inquiry> findByCategoryOrderByCreatedAtDesc(Inquiry.InquiryCategory category, Pageable pageable);

    // 상태별 조회
    Page<Inquiry> findByStatusOrderByCreatedAtDesc(Inquiry.InquiryStatus status, Pageable pageable);

    // 회원의 미답변 문의 개수
    Long countByMemberIdAndStatus(Long memberId, Inquiry.InquiryStatus status);

    // 특정 회원의 모든 문의 삭제
    @Modifying
    @Query("DELETE FROM Inquiry i WHERE i.member.id = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}

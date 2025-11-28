package ac.inhatc.reservation_system.promotion.repository;

import ac.inhatc.reservation_system.promotion.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    
    // 전체 홍보글 조회 (최신순)
    Page<Promotion> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // 전체 홍보글 조회 (인기순 - 조회수)
    Page<Promotion> findAllByOrderByViewCountDesc(Pageable pageable);
    
    // 전체 홍보글 조회 (좋아요순)
    Page<Promotion> findAllByOrderByLikeCountDesc(Pageable pageable);
    
    // 카테고리별 조회
    Page<Promotion> findByCategoryOrderByCreatedAtDesc(Promotion.PromotionCategory category, Pageable pageable);
    
    // 내가 작성한 홍보글 조회
    Page<Promotion> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);
}

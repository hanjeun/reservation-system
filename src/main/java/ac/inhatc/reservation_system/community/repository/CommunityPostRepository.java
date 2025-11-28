package ac.inhatc.reservation_system.community.repository;

import ac.inhatc.reservation_system.community.entity.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {
    
    // 카테고리별 조회 (페이징)
    Page<CommunityPost> findByCategory(CommunityPost.PostCategory category, Pageable pageable);
    
    // 전체 조회 (페이징)
    Page<CommunityPost> findAll(Pageable pageable);
    
    // 제목으로 검색
    Page<CommunityPost> findByTitleContaining(String keyword, Pageable pageable);
    
    // 제목 또는 내용으로 검색
    @Query("SELECT p FROM CommunityPost p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
    Page<CommunityPost> searchByTitleOrContent(@Param("keyword") String keyword, Pageable pageable);
    
    // 작성자별 게시글 조회
    Page<CommunityPost> findByAuthorId(Long authorId, Pageable pageable);
    
    // 인기 게시글 (좋아요 많은 순)
    List<CommunityPost> findTop10ByOrderByLikeCountDesc();
}

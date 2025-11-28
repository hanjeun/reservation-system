package ac.inhatc.reservation_system.community.repository;

import ac.inhatc.reservation_system.community.entity.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {
    
    // 게시글별 댓글 조회
    List<CommunityComment> findByPostIdOrderByCreatedAtAsc(Long postId);
    
    // 게시글별 댓글 개수
    Long countByPostId(Long postId);
}

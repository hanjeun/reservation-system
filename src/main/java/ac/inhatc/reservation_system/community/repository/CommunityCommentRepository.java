package ac.inhatc.reservation_system.community.repository;

import ac.inhatc.reservation_system.community.entity.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

    // 게시글별 댓글 조회
    List<CommunityComment> findByPostIdOrderByCreatedAtAsc(Long postId);

    // 게시글별 댓글 개수
    Long countByPostId(Long postId);

    // 특정 게시글들의 모든 댓글 삭제
    @Modifying
    @Query("DELETE FROM CommunityComment c WHERE c.post.id IN :postIds")
    void deleteByPostIds(@Param("postIds") List<Long> postIds);

    // 특정 회원의 모든 댓글 삭제
    @Modifying
    @Query("DELETE FROM CommunityComment c WHERE c.author.id = :memberId")
    void deleteByAuthorId(@Param("memberId") Long memberId);
}

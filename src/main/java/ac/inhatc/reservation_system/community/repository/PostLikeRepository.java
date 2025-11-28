package ac.inhatc.reservation_system.community.repository;

import ac.inhatc.reservation_system.community.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    
    // 특정 사용자가 특정 게시글에 좋아요를 눌렀는지 확인
    Optional<PostLike> findByPostIdAndMemberId(Long postId, Long memberId);
    
    // 특정 사용자가 특정 게시글에 좋아요를 눌렀는지 여부
    boolean existsByPostIdAndMemberId(Long postId, Long memberId);
    
    // 특정 게시글의 좋아요 개수
    Long countByPostId(Long postId);
}

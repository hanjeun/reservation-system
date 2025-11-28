package ac.inhatc.reservation_system.community.controller;

import ac.inhatc.reservation_system.community.dto.CommunityDto;
import ac.inhatc.reservation_system.community.service.CommunityService;
import ac.inhatc.reservation_system.config.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityApiController {

    private final CommunityService communityService;

    // 게시글 목록 조회
    @GetMapping("/posts")
    public ResponseEntity<Page<CommunityDto.PostResponse>> getPosts(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<CommunityDto.PostResponse> posts = communityService.getPosts(category, page, size);
        return ResponseEntity.ok(posts);
    }

    // 게시글 검색
    @GetMapping("/posts/search")
    public ResponseEntity<Page<CommunityDto.PostResponse>> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<CommunityDto.PostResponse> posts = communityService.searchPosts(keyword, page, size);
        return ResponseEntity.ok(posts);
    }

    // 게시글 상세 조회
    @GetMapping("/posts/{postId}")
    public ResponseEntity<CommunityDto.PostResponse> getPost(@PathVariable Long postId) {
        try {
            Long memberId = SecurityUtil.getCurrentMemberId();
            CommunityDto.PostResponse post = communityService.getPost(postId, memberId);
            return ResponseEntity.ok(post);
        } catch (IllegalStateException e) {
            // 로그인하지 않은 경우 - memberId를 null로 처리
            CommunityDto.PostResponse post = communityService.getPost(postId, null);
            return ResponseEntity.ok(post);
        }
    }

    // 게시글 작성
    @PostMapping("/posts")
    public ResponseEntity<CommunityDto.PostResponse> createPost(@RequestBody CommunityDto.PostRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        CommunityDto.PostResponse post = communityService.createPost(memberId, request);
        return ResponseEntity.ok(post);
    }

    // 게시글 수정
    @PutMapping("/posts/{postId}")
    public ResponseEntity<CommunityDto.PostResponse> updatePost(
            @PathVariable Long postId,
            @RequestBody CommunityDto.PostRequest request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        CommunityDto.PostResponse post = communityService.updatePost(postId, memberId, request);
        return ResponseEntity.ok(post);
    }

    // 게시글 삭제
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        try {
            Long memberId = SecurityUtil.getCurrentMemberId();
            System.out.println("게시글 삭제 요청 - 게시글 ID: " + postId + ", 사용자 ID: " + memberId);
            communityService.deletePost(postId, memberId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            System.err.println("게시글 삭제 실패 - 인증 오류: " + e.getMessage());
            return ResponseEntity.status(401).build();
        } catch (IllegalArgumentException e) {
            System.err.println("게시글 삭제 실패 - 권한 오류: " + e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            System.err.println("게시글 삭제 실패 - 일반 오류: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // 댓글 목록 조회
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommunityDto.CommentResponse>> getComments(@PathVariable Long postId) {
        try {
            Long memberId = SecurityUtil.getCurrentMemberId();
            List<CommunityDto.CommentResponse> comments = communityService.getComments(postId, memberId);
            return ResponseEntity.ok(comments);
        } catch (IllegalStateException e) {
            // 로그인하지 않은 경우
            List<CommunityDto.CommentResponse> comments = communityService.getComments(postId, null);
            return ResponseEntity.ok(comments);
        }
    }

    // 댓글 작성
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommunityDto.CommentResponse> createComment(
            @PathVariable Long postId,
            @RequestBody CommunityDto.CommentRequest request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        CommunityDto.CommentResponse comment = communityService.createComment(postId, memberId, request);
        return ResponseEntity.ok(comment);
    }

    // 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        try {
            Long memberId = SecurityUtil.getCurrentMemberId();
            System.out.println("댓글 삭제 요청 - 댓글 ID: " + commentId + ", 사용자 ID: " + memberId);
            communityService.deleteComment(commentId, memberId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            System.err.println("댓글 삭제 실패 - 인증 오류: " + e.getMessage());
            return ResponseEntity.status(401).build();
        } catch (IllegalArgumentException e) {
            System.err.println("댓글 삭제 실패 - 권한 오류: " + e.getMessage());
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            System.err.println("댓글 삭제 실패 - 일반 오류: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    // 좋아요 토글
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<Boolean> toggleLike(@PathVariable Long postId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        boolean isLiked = communityService.toggleLike(postId, memberId);
        return ResponseEntity.ok(isLiked);
    }
}

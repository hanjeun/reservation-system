package ac.inhatc.reservation_system.community.service;

import ac.inhatc.reservation_system.community.dto.CommunityDto;
import ac.inhatc.reservation_system.community.entity.CommunityComment;
import ac.inhatc.reservation_system.community.entity.CommunityPost;
import ac.inhatc.reservation_system.community.entity.PostLike;
import ac.inhatc.reservation_system.community.repository.CommunityCommentRepository;
import ac.inhatc.reservation_system.community.repository.CommunityPostRepository;
import ac.inhatc.reservation_system.community.repository.PostLikeRepository;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final MemberRepository memberRepository;

    // 게시글 목록 조회 (페이징)
    public Page<CommunityDto.PostResponse> getPosts(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<CommunityPost> posts;
        if (category == null || category.equals("ALL")) {
            posts = postRepository.findAll(pageable);
        } else {
            CommunityPost.PostCategory postCategory = CommunityPost.PostCategory.valueOf(category);
            posts = postRepository.findByCategory(postCategory, pageable);
        }
        
        return posts.map(CommunityDto.PostResponse::from);
    }

    // 게시글 검색
    public Page<CommunityDto.PostResponse> searchPosts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CommunityPost> posts = postRepository.searchByTitleOrContent(keyword, pageable);
        return posts.map(CommunityDto.PostResponse::from);
    }

    // 게시글 상세 조회
    @Transactional
    public CommunityDto.PostResponse getPost(Long postId, Long memberId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        
        // 조회수 증가
        post.incrementViewCount();
        
        // 로그인하지 않은 경우 memberId가 null일 수 있음
        if (memberId == null) {
            return CommunityDto.PostResponse.from(post, -1L, false);
        }
        
        // 좋아요 여부 확인
        boolean isLiked = postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
        
        return CommunityDto.PostResponse.from(post, memberId, isLiked);
    }

    // 게시글 작성
    @Transactional
    public CommunityDto.PostResponse createPost(Long memberId, CommunityDto.PostRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        CommunityPost post = CommunityPost.builder()
                .author(member)
                .title(request.getTitle())
                .content(request.getContent())
                .category(CommunityPost.PostCategory.valueOf(request.getCategory()))
                .build();

        CommunityPost savedPost = postRepository.save(post);
        return CommunityDto.PostResponse.from(savedPost, memberId, false);
    }

    // 게시글 수정
    @Transactional
    public CommunityDto.PostResponse updatePost(Long postId, Long memberId, CommunityDto.PostRequest request) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(memberId)) {
            throw new IllegalArgumentException("게시글 작성자만 수정할 수 있습니다.");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setCategory(CommunityPost.PostCategory.valueOf(request.getCategory()));

        boolean isLiked = postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
        return CommunityDto.PostResponse.from(post, memberId, isLiked);
    }

    // 게시글 삭제
    @Transactional
    public void deletePost(Long postId, Long memberId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!post.getAuthor().getId().equals(memberId)) {
            System.out.println("게시글 삭제 권한 없음 - 게시글 작성자 ID: " + post.getAuthor().getId() + ", 현재 사용자 ID: " + memberId);
            throw new IllegalArgumentException("게시글 작성자만 삭제할 수 있습니다.");
        }

        postRepository.delete(post);
    }

    // 댓글 목록 조회
    public List<CommunityDto.CommentResponse> getComments(Long postId, Long currentUserId) {
        List<CommunityComment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        
        // 로그인하지 않은 경우 currentUserId가 null일 수 있음
        Long userId = currentUserId != null ? currentUserId : -1L;
        
        return comments.stream()
                .map(comment -> CommunityDto.CommentResponse.from(comment, userId))
                .collect(Collectors.toList());
    }

    // 댓글 작성
    @Transactional
    public CommunityDto.CommentResponse createComment(Long postId, Long memberId, CommunityDto.CommentRequest request) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .author(member)
                .content(request.getContent())
                .build();

        CommunityComment savedComment = commentRepository.save(comment);
        return CommunityDto.CommentResponse.from(savedComment, memberId);
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(Long commentId, Long memberId) {
        CommunityComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getAuthor().getId().equals(memberId)) {
            System.out.println("댓글 삭제 권한 없음 - 댓글 작성자 ID: " + comment.getAuthor().getId() + ", 현재 사용자 ID: " + memberId);
            throw new IllegalArgumentException("댓글 작성자만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
    }

    // 좋아요 토글 (추가/취소)
    @Transactional
    public boolean toggleLike(Long postId, Long memberId) {
        CommunityPost post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Optional<PostLike> existingLike = postLikeRepository.findByPostIdAndMemberId(postId, memberId);
        
        if (existingLike.isPresent()) {
            // 이미 좋아요를 눌렀다면 취소
            postLikeRepository.delete(existingLike.get());
            post.decrementLikeCount();
            return false; // 좋아요 취소됨
        } else {
            // 좋아요 추가
            PostLike postLike = PostLike.builder()
                    .post(post)
                    .member(member)
                    .build();
            postLikeRepository.save(postLike);
            post.incrementLikeCount();
            return true; // 좋아요 추가됨
        }
    }

    // 좋아요 상태 확인
    public boolean isLiked(Long postId, Long memberId) {
        return postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
    }
}

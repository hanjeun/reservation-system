package ac.inhatc.reservation_system.review.controller;

import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.review.dto.ReviewCreateRequest;
import ac.inhatc.reservation_system.review.dto.ReviewResponse;
import ac.inhatc.reservation_system.review.dto.ReviewUpdateRequest;
import ac.inhatc.reservation_system.review.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reviews")
public class ReviewApiController {

    private final ReviewService reviewService;

    /**
     * 리뷰 작성
     */
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @Valid @RequestBody ReviewCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");

        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("📝 리뷰 작성 요청: reservationId={}, memberId={}", request.getReservationId(), member.getId());

        try {
            ReviewResponse review = reviewService.createReview(request, member);
            return ResponseEntity.status(HttpStatus.CREATED).body(review);
        } catch (IllegalArgumentException e) {
            log.error("❌ 리뷰 작성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 가게의 리뷰 목록 조회
     */
    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<ReviewResponse>> getStoreReviews(@PathVariable Long storeId) {
        log.info("📋 가게 리뷰 목록 조회: storeId={}", storeId);

        List<ReviewResponse> reviews = reviewService.getStoreReviews(storeId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * 가게의 리뷰 통계 조회
     */
    @GetMapping("/store/{storeId}/stats")
    public ResponseEntity<Map<String, Object>> getStoreReviewStats(@PathVariable Long storeId) {
        log.info("📊 가게 리뷰 통계 조회: storeId={}", storeId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("averageRating", reviewService.getAverageRating(storeId));
        stats.put("reviewCount", reviewService.getReviewCount(storeId));

        return ResponseEntity.ok(stats);
    }

    /**
     * 내 리뷰 목록 조회
     */
    @GetMapping("/my")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(HttpServletRequest httpRequest) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");

        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("📋 내 리뷰 목록 조회: memberId={}", member.getId());

        List<ReviewResponse> reviews = reviewService.getMyReviews(member);
        return ResponseEntity.ok(reviews);
    }

    /**
     * 리뷰 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");

        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("🗑️ 리뷰 삭제 요청: reviewId={}, memberId={}", id, member.getId());

        try {
            reviewService.deleteReview(id, member);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("❌ 리뷰 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 리뷰 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ReviewUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");

        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("✏️ 리뷰 수정 요청: reviewId={}, memberId={}", id, member.getId());

        try {
            ReviewResponse review = reviewService.updateReview(id, request, member);
            return ResponseEntity.ok(review);
        } catch (IllegalArgumentException e) {
            log.error("❌ 리뷰 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 단일 리뷰 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable Long id) {
        log.info("📋 리뷰 조회: reviewId={}", id);

        try {
            ReviewResponse review = reviewService.getReview(id);
            return ResponseEntity.ok(review);
        } catch (IllegalArgumentException e) {
            log.error("❌ 리뷰 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 예약 ID로 리뷰 조회
     */
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<ReviewResponse> getReviewByReservation(@PathVariable Long reservationId) {
        log.info("📋 예약 ID로 리뷰 조회: reservationId={}", reservationId);

        ReviewResponse review = reviewService.getReviewByReservationId(reservationId);
        if (review == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(review);
    }

    /**
     * 리뷰 작성 가능 여부 확인
     */
    @GetMapping("/can-write/{reservationId}")
    public ResponseEntity<Map<String, Boolean>> canWriteReview(
            @PathVariable Long reservationId,
            HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");

        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        boolean canWrite = reviewService.canWriteReview(reservationId, member);
        Map<String, Boolean> response = new HashMap<>();
        response.put("canWrite", canWrite);

        return ResponseEntity.ok(response);
    }
}

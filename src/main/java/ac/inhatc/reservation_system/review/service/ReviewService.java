package ac.inhatc.reservation_system.review.service;

import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.reservation.entity.Reservation;
import ac.inhatc.reservation_system.reservation.repository.ReservationRepository;
import ac.inhatc.reservation_system.review.dto.ReviewCreateRequest;
import ac.inhatc.reservation_system.review.dto.ReviewResponse;
import ac.inhatc.reservation_system.review.dto.ReviewUpdateRequest;
import ac.inhatc.reservation_system.review.entity.Review;
import ac.inhatc.reservation_system.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;

    /**
     * 리뷰 작성
     */
    @Transactional
    public ReviewResponse createReview(ReviewCreateRequest request, Member member) {
        log.info("📝 리뷰 작성 시작: reservationId={}, memberId={}", request.getReservationId(), member.getId());

        // 예약 조회
        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다."));

        // 본인 예약인지 확인
        if (!reservation.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("본인의 예약에만 리뷰를 작성할 수 있습니다.");
        }

        // 이용완료 상태인지 확인
        if (reservation.getStatus() != Reservation.ReservationStatus.COMPLETED) {
            throw new IllegalArgumentException("이용완료된 예약에만 리뷰를 작성할 수 있습니다.");
        }

        // 이미 리뷰가 존재하는지 확인
        if (reviewRepository.existsByReservationId(reservation.getId())) {
            throw new IllegalArgumentException("이미 리뷰를 작성한 예약입니다.");
        }

        // 리뷰 생성
        Review review = Review.builder()
                .member(member)
                .store(reservation.getStore())
                .reservation(reservation)
                .rating(request.getRating())
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("✅ 리뷰 작성 완료: reviewId={}", savedReview.getId());

        return ReviewResponse.from(savedReview);
    }

    /**
     * 가게의 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getStoreReviews(Long storeId) {
        log.info("📋 가게 리뷰 목록 조회: storeId={}", storeId);

        List<Review> reviews = reviewRepository.findByStoreIdOrderByCreatedAtDesc(storeId);

        return reviews.stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 내 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(Member member) {
        log.info("📋 내 리뷰 목록 조회: memberId={}", member.getId());

        List<Review> reviews = reviewRepository.findByMemberOrderByCreatedAtDesc(member);

        return reviews.stream()
                .map(ReviewResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 리뷰 삭제
     */
    @Transactional
    public void deleteReview(Long reviewId, Member member) {
        log.info("🗑️ 리뷰 삭제: reviewId={}, memberId={}", reviewId, member.getId());

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        // 본인 리뷰인지 확인
        if (!review.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("본인의 리뷰만 삭제할 수 있습니다.");
        }

        reviewRepository.delete(review);
        log.info("✅ 리뷰 삭제 완료: reviewId={}", reviewId);
    }

    /**
     * 리뷰 수정
     */
    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewUpdateRequest request, Member member) {
        log.info("✏️ 리뷰 수정: reviewId={}, memberId={}", reviewId, member.getId());

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        // 본인 리뷰인지 확인
        if (!review.getMember().getId().equals(member.getId())) {
            throw new IllegalArgumentException("본인의 리뷰만 수정할 수 있습니다.");
        }

        review.update(request.getRating(), request.getTitle(), request.getContent());
        log.info("✅ 리뷰 수정 완료: reviewId={}", reviewId);

        return ReviewResponse.from(review);
    }

    /**
     * 단일 리뷰 조회
     */
    @Transactional(readOnly = true)
    public ReviewResponse getReview(Long reviewId) {
        log.info("📋 리뷰 조회: reviewId={}", reviewId);

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        return ReviewResponse.from(review);
    }

    /**
     * 예약 ID로 리뷰 조회
     */
    @Transactional(readOnly = true)
    public ReviewResponse getReviewByReservationId(Long reservationId) {
        log.info("📋 예약 ID로 리뷰 조회: reservationId={}", reservationId);

        Review review = reviewRepository.findByReservationId(reservationId)
                .orElse(null);

        return review != null ? ReviewResponse.from(review) : null;
    }

    /**
     * 가게의 평균 별점 조회
     */
    @Transactional(readOnly = true)
    public Double getAverageRating(Long storeId) {
        Double avg = reviewRepository.findAverageRatingByStoreId(storeId);
        return avg != null ? Math.round(avg * 10) / 10.0 : 0.0;
    }

    /**
     * 가게의 리뷰 개수 조회
     */
    @Transactional(readOnly = true)
    public long getReviewCount(Long storeId) {
        return reviewRepository.countByStoreId(storeId);
    }

    /**
     * 예약에 대한 리뷰 작성 가능 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean canWriteReview(Long reservationId, Member member) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        
        if (reservation == null) {
            return false;
        }

        // 본인 예약인지 확인
        if (!reservation.getMember().getId().equals(member.getId())) {
            return false;
        }

        // 이용완료 상태인지 확인
        if (reservation.getStatus() != Reservation.ReservationStatus.COMPLETED) {
            return false;
        }

        // 이미 리뷰가 존재하는지 확인
        return !reviewRepository.existsByReservationId(reservationId);
    }
}

package ac.inhatc.reservation_system.review.repository;

import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.reservation.entity.Reservation;
import ac.inhatc.reservation_system.review.entity.Review;
import ac.inhatc.reservation_system.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // 가게의 리뷰 목록 조회 (최신순)
    List<Review> findByStoreOrderByCreatedAtDesc(Store store);

    // 가게의 리뷰 목록 조회 (가게 ID로)
    List<Review> findByStoreIdOrderByCreatedAtDesc(Long storeId);

    // 회원의 리뷰 목록 조회
    List<Review> findByMemberOrderByCreatedAtDesc(Member member);

    // 예약에 대한 리뷰 조회
    Optional<Review> findByReservation(Reservation reservation);

    // 예약 ID로 리뷰 존재 여부 확인
    boolean existsByReservationId(Long reservationId);

    // 예약 ID로 리뷰 조회
    Optional<Review> findByReservationId(Long reservationId);

    // 가게의 평균 별점 조회
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.store.id = :storeId")
    Double findAverageRatingByStoreId(@Param("storeId") Long storeId);

    // 가게의 리뷰 개수 조회
    long countByStoreId(Long storeId);

    // 특정 가게의 모든 리뷰 삭제
    @Modifying
    @Query("DELETE FROM Review r WHERE r.store.id = :storeId")
    void deleteByStoreId(@Param("storeId") Long storeId);

    // 특정 회원의 모든 리뷰 삭제
    @Modifying
    @Query("DELETE FROM Review r WHERE r.member.id = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}

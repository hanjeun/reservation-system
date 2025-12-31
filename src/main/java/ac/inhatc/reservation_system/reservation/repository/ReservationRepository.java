package ac.inhatc.reservation_system.reservation.repository;

import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.reservation.entity.Reservation;
import ac.inhatc.reservation_system.store.entity.Store;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * 특정 회원의 예약 내역 조회 (최신순)
     */
    List<Reservation> findByMemberOrderByCreatedAtDesc(Member member);

    /**
     * 특정 가게의 예약 내역 조회 (최신순)
     */
    List<Reservation> findByStoreOrderByCreatedAtDesc(Store store);

    /**
     * 특정 가게의 모든 예약 삭제
     */
    @Modifying
    @Query("DELETE FROM Reservation r WHERE r.store.id = :storeId")
    void deleteByStoreId(@Param("storeId") Long storeId);

    /**
     * 특정 회원의 모든 예약 삭제
     */
    @Modifying
    @Query("DELETE FROM Reservation r WHERE r.member.id = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);

    /**
     * 가게별 예약 목록 조회 (페이징)
     */
    Page<Reservation> findByStoreOrderByReservationDateDescReservationTimeDesc(Store store, Pageable pageable);

    /**
     * 가게별 예약자 이름으로 검색
     */
    @Query("SELECT r FROM Reservation r WHERE r.store = :store AND r.member.name LIKE %:keyword% ORDER BY r.reservationDate DESC, r.reservationTime DESC")
    Page<Reservation> searchByMemberName(@Param("store") Store store, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 가게별 예약자 이메일로 검색
     */
    @Query("SELECT r FROM Reservation r WHERE r.store = :store AND r.member.email LIKE %:keyword% ORDER BY r.reservationDate DESC, r.reservationTime DESC")
    Page<Reservation> searchByMemberEmail(@Param("store") Store store, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 가게별 예약 날짜로 검색
     */
    @Query("SELECT r FROM Reservation r WHERE r.store = :store AND r.reservationDate = :date ORDER BY r.reservationTime DESC")
    Page<Reservation> searchByReservationDate(@Param("store") Store store, @Param("date") LocalDate date, Pageable pageable);

    /**
     * 가게별 예약 상태로 검색
     */
    @Query("SELECT r FROM Reservation r WHERE r.store = :store AND r.status = :status ORDER BY r.reservationDate DESC, r.reservationTime DESC")
    Page<Reservation> searchByStatus(@Param("store") Store store, @Param("status") Reservation.ReservationStatus status, Pageable pageable);

    /**
     * 가게별 복합 검색 (이름 또는 이메일)
     */
    @Query("SELECT r FROM Reservation r WHERE r.store = :store AND (r.member.name LIKE %:keyword% OR r.member.email LIKE %:keyword%) ORDER BY r.reservationDate DESC, r.reservationTime DESC")
    Page<Reservation> searchByKeyword(@Param("store") Store store, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 가게별 복합 검색 (키워드 + 상태)
     */
    @Query("SELECT r FROM Reservation r WHERE r.store = :store AND r.status = :status AND (r.member.name LIKE %:keyword% OR r.member.email LIKE %:keyword%) ORDER BY r.reservationDate DESC, r.reservationTime DESC")
    Page<Reservation> searchByKeywordAndStatus(@Param("store") Store store, @Param("keyword") String keyword, @Param("status") Reservation.ReservationStatus status, Pageable pageable);

    /**
     * 가게별 복합 검색 (날짜 범위)
     */
    @Query("SELECT r FROM Reservation r WHERE r.store = :store AND r.reservationDate BETWEEN :startDate AND :endDate ORDER BY r.reservationDate DESC, r.reservationTime DESC")
    Page<Reservation> searchByDateRange(@Param("store") Store store, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);

    /**
     * 가게별 복합 검색 (키워드 + 날짜 범위)
     */
    @Query("SELECT r FROM Reservation r WHERE r.store = :store AND r.reservationDate BETWEEN :startDate AND :endDate AND (r.member.name LIKE %:keyword% OR r.member.email LIKE %:keyword%) ORDER BY r.reservationDate DESC, r.reservationTime DESC")
    Page<Reservation> searchByKeywordAndDateRange(@Param("store") Store store, @Param("keyword") String keyword, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, Pageable pageable);
}

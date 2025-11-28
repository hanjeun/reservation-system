package ac.inhatc.reservation_system.reservation.repository;

import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.reservation.entity.Reservation;
import ac.inhatc.reservation_system.store.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

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
}

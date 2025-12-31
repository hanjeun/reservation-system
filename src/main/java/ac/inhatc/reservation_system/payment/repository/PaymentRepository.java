package ac.inhatc.reservation_system.payment.repository;

import ac.inhatc.reservation_system.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    // 가맹점 주문번호로 조회
    Optional<Payment> findByMerchantUid(String merchantUid);
    
    // 포트원 결제번호로 조회
    Optional<Payment> findByImpUid(String impUid);
    
    // 예약 ID로 조회
    Optional<Payment> findByReservationId(Long reservationId);
    
    // 회원 ID로 결제 목록 조회
    List<Payment> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    
    // 결제 상태로 조회
    List<Payment> findByStatus(Payment.PaymentStatus status);
    
    // 회원 ID와 결제 상태로 조회
    List<Payment> findByMemberIdAndStatus(Long memberId, Payment.PaymentStatus status);
    
    // 가게 ID로 결제 목록 조회 (사업자용)
    @Query("SELECT p FROM Payment p JOIN p.reservation r WHERE r.store.id = :storeId ORDER BY p.createdAt DESC")
    List<Payment> findByStoreId(@Param("storeId") Long storeId);
}

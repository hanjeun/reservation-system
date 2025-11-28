package ac.inhatc.reservation_system.reservation.entity;

import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.store.entity.Store;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reservation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    // 예약한 사용자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 예약한 가게
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    // 예약 날짜
    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    // 예약 시간
    @Column(name = "reservation_time", nullable = false)
    private LocalTime reservationTime;

    // 예약 인원
    @Column(name = "guest_count", nullable = false)
    private Integer guestCount;

    // 예약 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    // 요청 사항
    @Column(name = "special_request", length = 500)
    private String specialRequest;
    
    // 거절 사유 (사업자가 예약 거절 시 작성)
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ReservationStatus {
        PENDING,    // 대기중 (사용자가 예약 신청)
        CONFIRMED,  // 승인됨 (사업자가 승인)
        REJECTED,   // 거절됨 (사업자가 거절)
        CANCELLED   // 취소됨 (사용자 또는 사업자가 취소)
    }
}

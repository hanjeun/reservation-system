package ac.inhatc.reservation_system.reservation.dto;

import ac.inhatc.reservation_system.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {
    
    private Long id;
    private Long storeId;
    private String storeName;
    private String storeMainImageUrl;
    private Long memberId;
    private String memberName;
    private String memberEmail;
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private Integer guestCount;
    private String status;
    private String specialRequest;
    private String rejectionReason;
    
    // 결제 관련 필드
    private Boolean depositPaid;
    private Integer depositAmount;
    private Integer noShowDeposit;  // 가게의 노쇼방지금 설정 금액
    
    public static ReservationResponse from(Reservation reservation) {
        return ReservationResponse.builder()
                .id(reservation.getId())
                .storeId(reservation.getStore().getId())
                .storeName(reservation.getStore().getName())
                .storeMainImageUrl(reservation.getStore().getMainImageUrl())
                .memberId(reservation.getMember().getId())
                .memberName(reservation.getMember().getName())
                .memberEmail(reservation.getMember().getEmail())
                .reservationDate(reservation.getReservationDate())
                .reservationTime(reservation.getReservationTime())
                .guestCount(reservation.getGuestCount())
                .status(reservation.getStatus().name())
                .specialRequest(reservation.getSpecialRequest())
                .rejectionReason(reservation.getRejectionReason())
                .depositPaid(reservation.getDepositPaid())
                .depositAmount(reservation.getDepositAmount())
                .noShowDeposit(reservation.getStore().getNoShowDeposit())
                .build();
    }
}

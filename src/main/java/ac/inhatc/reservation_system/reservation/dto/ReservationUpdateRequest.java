package ac.inhatc.reservation_system.reservation.dto;

import ac.inhatc.reservation_system.reservation.entity.Reservation.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationUpdateRequest {
    
    private LocalDate reservationDate;
    private LocalTime reservationTime;
    private Integer guestCount;
    private String specialRequest;
    private ReservationStatus status;
}

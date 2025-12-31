package ac.inhatc.reservation_system.payment.service;

import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.repository.MemberRepository;
import ac.inhatc.reservation_system.payment.dto.*;
import ac.inhatc.reservation_system.payment.entity.Payment;
import ac.inhatc.reservation_system.payment.repository.PaymentRepository;
import ac.inhatc.reservation_system.reservation.entity.Reservation;
import ac.inhatc.reservation_system.reservation.repository.ReservationRepository;
import ac.inhatc.reservation_system.store.entity.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final PortoneService portoneService;

    /**
     * 결제 준비 (가맹점 주문번호 생성 및 결제 정보 저장)
     */
    public PaymentPrepareDto preparePayment(PaymentRequestDto requestDto, Long memberId) {
        // 예약 정보 조회
        Reservation reservation = reservationRepository.findById(requestDto.getReservationId())
                .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다."));

        // 회원 정보 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));

        // 이미 결제된 예약인지 확인
        if (reservation.getDepositPaid()) {
            throw new IllegalStateException("이미 결제가 완료된 예약입니다.");
        }

        // 가맹점 주문번호 생성 (고유값)
        String merchantUid = generateMerchantUid();

        // 결제 정보 저장 (READY 상태)
        Payment payment = Payment.builder()
                .member(member)
                .reservation(reservation)
                .merchantUid(merchantUid)
                .amount(requestDto.getAmount())
                .productName(requestDto.getProductName())
                .buyerName(requestDto.getBuyerName() != null ? requestDto.getBuyerName() : member.getName())
                .buyerEmail(requestDto.getBuyerEmail() != null ? requestDto.getBuyerEmail() : member.getEmail())
                .buyerTel(requestDto.getBuyerTel())
                .pgProvider(requestDto.getPgProvider())
                .status(Payment.PaymentStatus.READY)
                .build();

        paymentRepository.save(payment);

        // 프론트엔드로 전달할 결제 준비 정보
        return PaymentPrepareDto.builder()
                .merchantUid(merchantUid)
                .amount(requestDto.getAmount())
                .productName(requestDto.getProductName())
                .buyerName(payment.getBuyerName())
                .buyerEmail(payment.getBuyerEmail())
                .buyerTel(payment.getBuyerTel())
                .impCode(portoneService.getImpCode())
                .pgProvider(requestDto.getPgProvider())
                .reservationId(reservation.getId())
                .build();
    }

    /**
     * 결제 검증 및 완료 처리
     */
    public PaymentResponseDto verifyAndCompletePayment(PaymentVerifyDto verifyDto) {
        // 결제 정보 조회
        Payment payment = paymentRepository.findByMerchantUid(verifyDto.getMerchantUid())
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        // 포트원 API로 결제 정보 확인
        PortonePaymentResponse.Response portonePayment = portoneService.getPaymentInfo(verifyDto.getImpUid());

        // 결제 금액 검증
        if (portonePayment.getAmount() != payment.getAmount()) {
            log.error("결제 금액 불일치 - DB: {}, 포트원: {}", payment.getAmount(), portonePayment.getAmount());
            payment.failPayment("결제 금액 불일치");
            paymentRepository.save(payment);
            throw new IllegalStateException("결제 금액이 일치하지 않습니다.");
        }

        // 결제 상태 확인
        if (!"paid".equals(portonePayment.getStatus())) {
            log.error("결제 상태 이상 - status: {}", portonePayment.getStatus());
            payment.failPayment("결제 상태 이상: " + portonePayment.getStatus());
            paymentRepository.save(payment);
            throw new IllegalStateException("결제가 완료되지 않았습니다.");
        }

        // 결제 완료 처리
        payment.completePayment(
                verifyDto.getImpUid(),
                portonePayment.getPayMethod(),
                portonePayment.getPgProvider()
        );
        paymentRepository.save(payment);

        // 예약 정보 업데이트
        Reservation reservation = payment.getReservation();
        reservation.markDepositPaid(payment.getAmount());
        reservationRepository.save(reservation);

        log.info("결제 완료 - merchantUid: {}, impUid: {}, amount: {}", 
                payment.getMerchantUid(), payment.getImpUid(), payment.getAmount());

        return PaymentResponseDto.fromEntity(payment);
    }

    /**
     * 결제 환불
     */
    public PaymentResponseDto refundPayment(PaymentRefundDto refundDto) {
        // 결제 정보 조회
        Payment payment;
        if (refundDto.getPaymentId() != null) {
            payment = paymentRepository.findById(refundDto.getPaymentId())
                    .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));
        } else {
            payment = paymentRepository.findByReservationId(refundDto.getReservationId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 예약의 결제 정보를 찾을 수 없습니다."));
        }

        // 환불 가능 상태 확인
        if (payment.getStatus() != Payment.PaymentStatus.PAID) {
            throw new IllegalStateException("환불 가능한 상태가 아닙니다.");
        }

        // 환불 금액 설정 (전액 환불 또는 부분 환불)
        Integer refundAmount = refundDto.getRefundAmount() != null ? 
                refundDto.getRefundAmount() : payment.getAmount();

        // 포트원 API로 환불 요청
        portoneService.cancelPayment(
                payment.getImpUid(),
                refundAmount,
                refundDto.getRefundReason()
        );

        // 환불 처리
        payment.refundPayment(refundAmount, refundDto.getRefundReason());
        paymentRepository.save(payment);

        // 예약 정보 업데이트 (전액 환불 시)
        if (refundAmount.equals(payment.getAmount())) {
            Reservation reservation = payment.getReservation();
            reservation.setDepositPaid(false);
            reservation.setDepositAmount(0);
            reservationRepository.save(reservation);
        }

        log.info("환불 완료 - merchantUid: {}, refundAmount: {}", payment.getMerchantUid(), refundAmount);

        return PaymentResponseDto.fromEntity(payment);
    }

    /**
     * 결제 취소 (결제 완료 전 취소)
     */
    public void cancelPayment(String merchantUid) {
        Payment payment = paymentRepository.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));

        if (payment.getStatus() != Payment.PaymentStatus.READY) {
            throw new IllegalStateException("취소 가능한 상태가 아닙니다.");
        }

        payment.cancelPayment();
        paymentRepository.save(payment);

        log.info("결제 취소 - merchantUid: {}", merchantUid);
    }

    /**
     * 결제 정보 조회
     */
    @Transactional(readOnly = true)
    public PaymentResponseDto getPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다."));
        return PaymentResponseDto.fromEntity(payment);
    }

    /**
     * 예약 ID로 결제 정보 조회
     */
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentByReservation(Long reservationId) {
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("해당 예약의 결제 정보를 찾을 수 없습니다."));
        return PaymentResponseDto.fromEntity(payment);
    }

    /**
     * 회원별 결제 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsByMember(Long memberId) {
        return paymentRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(PaymentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 가게별 결제 목록 조회 (사업자용)
     */
    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsByStore(Long storeId) {
        return paymentRepository.findByStoreId(storeId)
                .stream()
                .map(PaymentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 가맹점 주문번호 생성
     */
    private String generateMerchantUid() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "ORDER_" + timestamp + "_" + uuid;
    }

    /**
     * 환불 정책에 따른 환불 금액 계산
     */
    public RefundCalculationResult calculateRefundAmount(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다."));

        Store store = reservation.getStore();
        LocalDate reservationDate = reservation.getReservationDate();
        LocalDate today = LocalDate.now();

        // 예약일까지 남은 일수
        long daysUntilReservation = ChronoUnit.DAYS.between(today, reservationDate);

        // 결제 금액 조회
        int paidAmount = reservation.getDepositAmount() != null ? reservation.getDepositAmount() : 0;
        
        if (paidAmount == 0) {
            return new RefundCalculationResult(0, 100, "결제 내역 없음");
        }

        // 환불 정책 가져오기 (기본값 포함)
        int fullRefundDays = store.getFullRefundDays() != null ? store.getFullRefundDays() : 3;
        int partialRefundDays = store.getPartialRefundDays() != null ? store.getPartialRefundDays() : 1;
        int partialRefundRate = store.getPartialRefundRate() != null ? store.getPartialRefundRate() : 50;

        int refundAmount;
        int refundRate;
        String reason;

        if (daysUntilReservation >= fullRefundDays) {
            // 전액 환불
            refundAmount = paidAmount;
            refundRate = 100;
            reason = String.format("예약일 %d일 전 취소 - 전액 환불", daysUntilReservation);
        } else if (daysUntilReservation >= partialRefundDays) {
            // 부분 환불
            refundAmount = (paidAmount * partialRefundRate) / 100;
            refundRate = partialRefundRate;
            reason = String.format("예약일 %d일 전 취소 - %d%% 환불", daysUntilReservation, partialRefundRate);
        } else {
            // 환불 불가
            refundAmount = 0;
            refundRate = 0;
            reason = String.format("예약일 %d일 전 취소 - 환불 불가", daysUntilReservation);
        }

        log.info("환불 금액 계산 - reservationId: {}, daysUntil: {}, refundRate: {}%, refundAmount: {}", 
                reservationId, daysUntilReservation, refundRate, refundAmount);

        return new RefundCalculationResult(refundAmount, refundRate, reason);
    }

    /**
     * 예약 취소 시 환불 처리 (환불 정책 적용)
     */
    public PaymentResponseDto refundByReservationCancel(Long reservationId) {
        // 결제 정보 조회
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElse(null);

        if (payment == null || payment.getStatus() != Payment.PaymentStatus.PAID) {
            log.info("환불할 결제 내역 없음 - reservationId: {}", reservationId);
            return null;
        }

        // 환불 금액 계산
        RefundCalculationResult refundCalc = calculateRefundAmount(reservationId);

        if (refundCalc.getRefundAmount() <= 0) {
            log.info("환불 불가 - reservationId: {}, reason: {}", reservationId, refundCalc.getReason());
            // 환불 불가 처리 (상태만 변경)
            payment.setStatus(Payment.PaymentStatus.CANCELLED);
            payment.setFailReason("환불 불가: " + refundCalc.getReason());
            paymentRepository.save(payment);
            return PaymentResponseDto.fromEntity(payment);
        }

        // 포트원 API로 환불 요청
        try {
            portoneService.cancelPayment(
                    payment.getImpUid(),
                    refundCalc.getRefundAmount(),
                    refundCalc.getReason()
            );

            // 환불 처리
            payment.refundPayment(refundCalc.getRefundAmount(), refundCalc.getReason());
            paymentRepository.save(payment);

            // 예약 정보 업데이트
            Reservation reservation = payment.getReservation();
            reservation.setDepositPaid(false);
            reservation.setDepositAmount(0);
            reservationRepository.save(reservation);

            log.info("환불 완료 - reservationId: {}, refundAmount: {}, reason: {}", 
                    reservationId, refundCalc.getRefundAmount(), refundCalc.getReason());

            return PaymentResponseDto.fromEntity(payment);
        } catch (Exception e) {
            log.error("환불 처리 실패 - reservationId: {}, error: {}", reservationId, e.getMessage());
            throw new RuntimeException("환불 처리에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 사업자 취소로 인한 전액 환불 (환불 정책 무시)
     */
    public PaymentResponseDto refundFullByOwnerCancel(Long reservationId) {
        // 결제 정보 조회
        Payment payment = paymentRepository.findByReservationId(reservationId)
                .orElse(null);

        if (payment == null || payment.getStatus() != Payment.PaymentStatus.PAID) {
            log.info("환불할 결제 내역 없음 - reservationId: {}", reservationId);
            return null;
        }

        int refundAmount = payment.getAmount();
        String reason = "사업자 취소로 인한 전액 환불";

        // 포트원 API로 환불 요청
        try {
            portoneService.cancelPayment(
                    payment.getImpUid(),
                    refundAmount,
                    reason
            );

            // 환불 처리
            payment.refundPayment(refundAmount, reason);
            paymentRepository.save(payment);

            // 예약 정보 업데이트
            Reservation reservation = payment.getReservation();
            reservation.setDepositPaid(false);
            reservation.setDepositAmount(0);
            reservationRepository.save(reservation);

            log.info("사업자 취소 전액 환불 완료 - reservationId: {}, refundAmount: {}",
                    reservationId, refundAmount);

            return PaymentResponseDto.fromEntity(payment);
        } catch (Exception e) {
            log.error("사업자 취소 환불 처리 실패 - reservationId: {}, error: {}", reservationId, e.getMessage());
            throw new RuntimeException("환불 처리에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 환불 계산 결과 DTO
     */
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class RefundCalculationResult {
        private int refundAmount;
        private int refundRate;
        private String reason;
    }
}

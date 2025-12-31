package ac.inhatc.reservation_system.payment.controller;

import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.payment.dto.*;
import ac.inhatc.reservation_system.payment.service.PaymentService;
import ac.inhatc.reservation_system.payment.service.PortoneService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PortoneService portoneService;

    /**
     * 결제 준비 (가맹점 주문번호 생성)
     */
    @PostMapping("/prepare")
    public ResponseEntity<PaymentPrepareDto> preparePayment(
            @RequestBody PaymentRequestDto requestDto,
            HttpServletRequest httpRequest) {
        
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        
        log.info("💳 결제 준비 요청: reservationId={}, memberId={}", requestDto.getReservationId(), member.getId());
        PaymentPrepareDto prepareDto = paymentService.preparePayment(requestDto, member.getId());
        
        return ResponseEntity.ok(prepareDto);
    }

    /**
     * 결제 검증 및 완료
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponseDto> verifyPayment(
            @RequestBody PaymentVerifyDto verifyDto,
            HttpServletRequest httpRequest) {
        
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        
        log.info("💳 결제 검증 요청: impUid={}, merchantUid={}", verifyDto.getImpUid(), verifyDto.getMerchantUid());
        PaymentResponseDto responseDto = paymentService.verifyAndCompletePayment(verifyDto);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 결제 환불
     */
    @PostMapping("/refund")
    public ResponseEntity<PaymentResponseDto> refundPayment(
            @RequestBody PaymentRefundDto refundDto,
            HttpServletRequest httpRequest) {
        
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        
        log.info("💳 환불 요청: paymentId={}", refundDto.getPaymentId());
        PaymentResponseDto responseDto = paymentService.refundPayment(refundDto);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 결제 취소 (결제 완료 전)
     */
    @PostMapping("/cancel/{merchantUid}")
    public ResponseEntity<Map<String, String>> cancelPayment(
            @PathVariable String merchantUid,
            HttpServletRequest httpRequest) {
        
        log.info("💳 결제 취소 요청: merchantUid={}", merchantUid);
        paymentService.cancelPayment(merchantUid);
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "결제가 취소되었습니다.");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 결제 정보 조회
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getPayment(@PathVariable Long paymentId) {
        PaymentResponseDto responseDto = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 예약 ID로 결제 정보 조회
     */
    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<PaymentResponseDto> getPaymentByReservation(@PathVariable Long reservationId) {
        PaymentResponseDto responseDto = paymentService.getPaymentByReservation(reservationId);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 내 결제 목록 조회
     */
    @GetMapping("/my-payments")
    public ResponseEntity<List<PaymentResponseDto>> getMyPayments(HttpServletRequest httpRequest) {
        
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        
        List<PaymentResponseDto> payments = paymentService.getPaymentsByMember(member.getId());
        return ResponseEntity.ok(payments);
    }

    /**
     * 가게별 결제 목록 조회 (사업자용)
     */
    @GetMapping("/store/{storeId}")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByStore(
            @PathVariable Long storeId,
            HttpServletRequest httpRequest) {
        
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        
        // 사업자 권한 체크
        if (!member.isBusiness() && !member.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        List<PaymentResponseDto> payments = paymentService.getPaymentsByStore(storeId);
        return ResponseEntity.ok(payments);
    }

    /**
     * 포트원 설정 정보 조회 (프론트엔드용)
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getPaymentConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("impCode", portoneService.getImpCode());
        return ResponseEntity.ok(config);
    }

    /**
     * 환불 금액 계산 (예약 취소 전 미리보기)
     */
    @GetMapping("/refund-preview/{reservationId}")
    public ResponseEntity<Map<String, Object>> getRefundPreview(
            @PathVariable Long reservationId,
            HttpServletRequest httpRequest) {

        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            PaymentService.RefundCalculationResult result = paymentService.calculateRefundAmount(reservationId);

            Map<String, Object> response = new HashMap<>();
            response.put("refundAmount", result.getRefundAmount());
            response.put("refundRate", result.getRefundRate());
            response.put("reason", result.getReason());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("환불 금액 계산 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}

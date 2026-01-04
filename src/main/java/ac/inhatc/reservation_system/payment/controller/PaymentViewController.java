package ac.inhatc.reservation_system.payment.controller;

import ac.inhatc.reservation_system.payment.dto.PaymentResponseDto;
import ac.inhatc.reservation_system.payment.dto.PaymentVerifyDto;
import ac.inhatc.reservation_system.payment.entity.Payment;
import ac.inhatc.reservation_system.payment.repository.PaymentRepository;
import ac.inhatc.reservation_system.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * 결제 관련 페이지 컨트롤러
 * 모바일/데스크탑 결제 결과 페이지 처리
 */
@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentViewController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    /**
     * 모바일 결제 완료 후 리다이렉트 처리
     * 카카오페이 등 모바일 결제 시 m_redirect_url로 설정된 URL
     */
    @GetMapping("/mobile-redirect")
    public String mobileRedirect(
            @RequestParam(value = "imp_uid", required = false) String impUid,
            @RequestParam(value = "merchant_uid", required = false) String merchantUid,
            @RequestParam(value = "imp_success", required = false) String impSuccess,
            @RequestParam(value = "success", required = false) String success,
            @RequestParam(value = "error_msg", required = false) String errorMsg,
            @RequestParam(value = "error_code", required = false) String errorCode,
            Model model) {

        log.info("📱 모바일 결제 리다이렉트: imp_uid={}, merchant_uid={}, imp_success={}, success={}",
                impUid, merchantUid, impSuccess, success);

        // imp_success 또는 success 파라미터로 성공 여부 확인
        boolean isSuccess = "true".equalsIgnoreCase(impSuccess) || "true".equalsIgnoreCase(success);

        if (!isSuccess) {
            // 결제 실패 또는 취소
            log.warn("📱 모바일 결제 실패/취소: {}", errorMsg);
            model.addAttribute("success", false);
            model.addAttribute("isCancelled", errorMsg != null && errorMsg.contains("취소"));
            model.addAttribute("message", errorMsg != null ? errorMsg : "결제가 취소되었습니다.");
            model.addAttribute("reason", errorMsg);
            model.addAttribute("errorCode", errorCode);
            model.addAttribute("merchantUid", merchantUid);
            return "payment/payment-result";
        }

        try {
            // merchantUid로 Payment 조회하여 reservationId 가져오기
            Optional<Payment> paymentOpt = paymentRepository.findByMerchantUid(merchantUid);
            if (paymentOpt.isEmpty()) {
                throw new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + merchantUid);
            }

            Payment payment = paymentOpt.get();
            Long reservationId = payment.getReservation().getId();

            // 결제 검증
            PaymentVerifyDto verifyDto = new PaymentVerifyDto();
            verifyDto.setImpUid(impUid);
            verifyDto.setMerchantUid(merchantUid);
            verifyDto.setReservationId(reservationId);

            PaymentResponseDto result = paymentService.verifyAndCompletePayment(verifyDto);

            model.addAttribute("success", true);
            model.addAttribute("message", "결제가 완료되었습니다.");
            model.addAttribute("reason", "노쇼 방지금이 결제되었습니다. 정상 방문 시 전액 환불됩니다.");
            model.addAttribute("payment", result);
            model.addAttribute("reservationId", reservationId);

            log.info("📱 모바일 결제 검증 성공: reservationId={}", reservationId);

        } catch (Exception e) {
            log.error("📱 모바일 결제 검증 실패: {}", e.getMessage(), e);
            model.addAttribute("success", false);
            model.addAttribute("isCancelled", false);
            model.addAttribute("message", "결제 검증에 실패했습니다.");
            model.addAttribute("reason", e.getMessage());
            model.addAttribute("merchantUid", merchantUid);
        }

        return "payment/payment-result";
    }

    /**
     * 데스크탑 결제 완료 페이지
     * JavaScript에서 결제 완료 후 리다이렉트
     */
    @GetMapping("/result")
    public String paymentResult(
            @RequestParam(value = "success", required = false, defaultValue = "false") boolean success,
            @RequestParam(value = "merchant_uid", required = false) String merchantUid,
            @RequestParam(value = "reservation_id", required = false) Long reservationId,
            @RequestParam(value = "message", required = false) String message,
            @RequestParam(value = "reason", required = false) String reason,
            @RequestParam(value = "error_code", required = false) String errorCode,
            @RequestParam(value = "cancelled", required = false, defaultValue = "false") boolean cancelled,
            Model model) {

        log.info("🖥️ 결제 결과 페이지: success={}, merchantUid={}, reservationId={}, cancelled={}",
                success, merchantUid, reservationId, cancelled);

        model.addAttribute("success", success);
        model.addAttribute("isCancelled", cancelled);
        model.addAttribute("merchantUid", merchantUid);
        model.addAttribute("errorCode", errorCode);

        if (success && merchantUid != null) {
            // 결제 성공 - 결제 정보 조회
            try {
                Optional<Payment> paymentOpt = paymentRepository.findByMerchantUid(merchantUid);
                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    model.addAttribute("payment", PaymentResponseDto.fromEntity(payment));
                    model.addAttribute("message", message != null ? message : "결제가 완료되었습니다.");
                    model.addAttribute("reason", reason != null ? reason : "노쇼 방지금이 결제되었습니다. 정상 방문 시 전액 환불됩니다.");
                }
            } catch (Exception e) {
                log.error("결제 정보 조회 실패: {}", e.getMessage());
            }
        } else {
            // 결제 실패/취소
            if (cancelled) {
                model.addAttribute("message", message != null ? message : "결제가 취소되었습니다.");
                model.addAttribute("reason", reason != null ? reason : "사용자가 결제를 취소했습니다.");
            } else {
                model.addAttribute("message", message != null ? message : "결제 처리 중 문제가 발생했습니다.");
                model.addAttribute("reason", reason);
            }
        }

        return "payment/payment-result";
    }

    /**
     * 결제 취소 전용 페이지
     */
    @GetMapping("/cancelled")
    public String paymentCancelled(
            @RequestParam(value = "merchant_uid", required = false) String merchantUid,
            @RequestParam(value = "reason", required = false) String reason,
            Model model) {

        log.info("🚫 결제 취소 페이지: merchantUid={}, reason={}", merchantUid, reason);

        model.addAttribute("success", false);
        model.addAttribute("isCancelled", true);
        model.addAttribute("merchantUid", merchantUid);
        model.addAttribute("message", "결제가 취소되었습니다.");
        model.addAttribute("reason", reason != null ? reason : "사용자가 결제를 취소했습니다.");

        return "payment/payment-result";
    }

    /**
     * 결제 실패 전용 페이지
     */
    @GetMapping("/failed")
    public String paymentFailed(
            @RequestParam(value = "merchant_uid", required = false) String merchantUid,
            @RequestParam(value = "error_msg", required = false) String errorMsg,
            @RequestParam(value = "error_code", required = false) String errorCode,
            Model model) {

        log.info("❌ 결제 실패 페이지: merchantUid={}, errorMsg={}, errorCode={}", merchantUid, errorMsg, errorCode);

        model.addAttribute("success", false);
        model.addAttribute("isCancelled", false);
        model.addAttribute("merchantUid", merchantUid);
        model.addAttribute("errorCode", errorCode);
        model.addAttribute("message", "결제에 실패했습니다.");
        model.addAttribute("reason", errorMsg != null ? errorMsg : "결제 처리 중 오류가 발생했습니다.");

        return "payment/payment-result";
    }
}

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
 * 모바일 결제 리다이렉트 처리
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
            Model model) {

        log.info("📱 모바일 결제 리다이렉트: imp_uid={}, merchant_uid={}, imp_success={}, success={}",
                impUid, merchantUid, impSuccess, success);

        // imp_success 또는 success 파라미터로 성공 여부 확인
        boolean isSuccess = "true".equalsIgnoreCase(impSuccess) || "true".equalsIgnoreCase(success);

        if (!isSuccess) {
            // 결제 실패 또는 취소
            log.warn("📱 모바일 결제 실패/취소: {}", errorMsg);
            model.addAttribute("success", false);
            model.addAttribute("message", errorMsg != null ? errorMsg : "결제가 취소되었습니다.");
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
            model.addAttribute("payment", result);
            model.addAttribute("reservationId", reservationId);

            log.info("📱 모바일 결제 검증 성공: reservationId={}", reservationId);

        } catch (Exception e) {
            log.error("📱 모바일 결제 검증 실패: {}", e.getMessage(), e);
            model.addAttribute("success", false);
            model.addAttribute("message", "결제 검증에 실패했습니다: " + e.getMessage());
            model.addAttribute("merchantUid", merchantUid);
        }

        return "payment/payment-result";
    }
}

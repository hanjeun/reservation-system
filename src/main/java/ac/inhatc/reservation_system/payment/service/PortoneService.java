package ac.inhatc.reservation_system.payment.service;

import ac.inhatc.reservation_system.payment.dto.PortonePaymentResponse;
import ac.inhatc.reservation_system.payment.dto.PortoneTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 포트원 API 연동 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PortoneService {

    private final RestTemplate restTemplate;

    @Value("${portone.imp-key}")
    private String impKey;

    @Value("${portone.imp-secret}")
    private String impSecret;

    @Value("${portone.imp-code}")
    private String impCode;

    private static final String PORTONE_API_URL = "https://api.iamport.kr";

    /**
     * 포트원 Access Token 발급
     */
    public String getAccessToken() {
        String url = PORTONE_API_URL + "/users/getToken";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("imp_key", impKey);
        requestBody.put("imp_secret", impSecret);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<PortoneTokenResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, PortoneTokenResponse.class);

            if (response.getBody() != null && response.getBody().getCode() == 0) {
                return response.getBody().getResponse().getAccessToken();
            } else {
                log.error("포트원 토큰 발급 실패: {}", response.getBody() != null ? response.getBody().getMessage() : "Unknown error");
                throw new RuntimeException("포트원 토큰 발급 실패");
            }
        } catch (Exception e) {
            log.error("포트원 API 호출 오류: {}", e.getMessage());
            throw new RuntimeException("포트원 API 호출 오류", e);
        }
    }

    /**
     * 결제 정보 조회
     */
    public PortonePaymentResponse.Response getPaymentInfo(String impUid) {
        String accessToken = getAccessToken();
        String url = PORTONE_API_URL + "/payments/" + impUid;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<PortonePaymentResponse> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, PortonePaymentResponse.class);

            if (response.getBody() != null && response.getBody().getCode() == 0) {
                return response.getBody().getResponse();
            } else {
                log.error("결제 정보 조회 실패: {}", response.getBody() != null ? response.getBody().getMessage() : "Unknown error");
                throw new RuntimeException("결제 정보 조회 실패");
            }
        } catch (Exception e) {
            log.error("결제 정보 조회 오류: {}", e.getMessage());
            throw new RuntimeException("결제 정보 조회 오류", e);
        }
    }

    /**
     * 결제 취소 (환불)
     */
    public PortonePaymentResponse.Response cancelPayment(String impUid, Integer amount, String reason) {
        log.info("🔄 포트원 환불 요청 시작 - impUid: {}, amount: {}, reason: {}", impUid, amount, reason);

        String accessToken = getAccessToken();
        String url = PORTONE_API_URL + "/payments/cancel";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("imp_uid", impUid);
        requestBody.put("reason", reason);

        if (amount != null) {
            requestBody.put("amount", amount);  // 부분 취소 시
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("📡 포트원 API 요청 - URL: {}, impUid: {}", url, impUid);

            ResponseEntity<PortonePaymentResponse> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, PortonePaymentResponse.class);

            log.info("📨 포트원 API 응답 - status: {}, body: {}",
                    response.getStatusCode(), response.getBody());

            if (response.getBody() != null && response.getBody().getCode() == 0) {
                log.info("✅ 환불 성공 - impUid: {}, amount: {}", impUid, amount);
                return response.getBody().getResponse();
            } else {
                String errorMsg = response.getBody() != null ? response.getBody().getMessage() : "Unknown error";
                log.error("❌ 포트원 환불 실패 - code: {}, message: {}",
                        response.getBody() != null ? response.getBody().getCode() : "null",
                        errorMsg);
                throw new RuntimeException("결제 취소 실패: " + errorMsg);
            }
        } catch (Exception e) {
            log.error("❌ 포트원 API 호출 오류 - impUid: {}, error: {}", impUid, e.getMessage(), e);
            throw new RuntimeException("결제 취소 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 결제 금액 검증
     */
    public boolean verifyPayment(String impUid, Integer expectedAmount) {
        try {
            PortonePaymentResponse.Response paymentInfo = getPaymentInfo(impUid);
            
            // 결제 상태가 paid이고, 금액이 일치하는지 확인
            boolean statusMatch = "paid".equals(paymentInfo.getStatus());
            boolean amountMatch = paymentInfo.getAmount() == expectedAmount;
            
            if (!statusMatch) {
                log.warn("결제 상태 불일치 - 예상: paid, 실제: {}", paymentInfo.getStatus());
            }
            if (!amountMatch) {
                log.warn("결제 금액 불일치 - 예상: {}, 실제: {}", expectedAmount, paymentInfo.getAmount());
            }
            
            return statusMatch && amountMatch;
        } catch (Exception e) {
            log.error("결제 검증 오류: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 가맹점 식별코드 반환
     */
    public String getImpCode() {
        return impCode;
    }
}

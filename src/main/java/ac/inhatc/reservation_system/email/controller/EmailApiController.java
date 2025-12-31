package ac.inhatc.reservation_system.email.controller;

import ac.inhatc.reservation_system.email.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailApiController {

    private final EmailVerificationService verificationService;

    /**
     * 인증 코드 발송
     */
    @PostMapping("/send-code")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");

            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이메일을 입력해주세요."
                ));
            }

            // 이메일 형식 검증
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "올바른 이메일 형식이 아닙니다."
                ));
            }

            verificationService.sendVerificationCode(email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "인증 코드가 발송되었습니다. 이메일을 확인해주세요."
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("인증 코드 발송 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "인증 코드 발송에 실패했습니다. 잠시 후 다시 시도해주세요."
            ));
        }
    }

    /**
     * 인증 코드 검증
     */
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String code = request.get("code");

            if (email == null || email.isBlank() || code == null || code.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "이메일과 인증 코드를 모두 입력해주세요."
                ));
            }

            verificationService.verifyCode(email, code);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "이메일 인증이 완료되었습니다."
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("인증 코드 검증 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "인증 처리 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 이메일 인증 상태 확인
     */
    @GetMapping("/check-verified")
    public ResponseEntity<?> checkVerified(@RequestParam String email) {
        try {
            boolean verified = verificationService.isEmailVerified(email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "verified", verified
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "verified", false
            ));
        }
    }
}

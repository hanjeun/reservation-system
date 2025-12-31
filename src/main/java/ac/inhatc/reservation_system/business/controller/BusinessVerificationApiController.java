package ac.inhatc.reservation_system.business.controller;

import ac.inhatc.reservation_system.business.dto.BusinessVerificationRequest;
import ac.inhatc.reservation_system.business.dto.BusinessVerificationResponse;
import ac.inhatc.reservation_system.business.entity.BusinessVerification.VerificationStatus;
import ac.inhatc.reservation_system.business.service.BusinessVerificationService;
import ac.inhatc.reservation_system.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/business-verification")
@RequiredArgsConstructor
public class BusinessVerificationApiController {

    private final BusinessVerificationService verificationService;

    /**
     * 사업자 인증 신청
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submitVerification(
            Authentication authentication,
            @ModelAttribute BusinessVerificationRequest request) {
        try {
            Member member = (Member) authentication.getPrincipal();
            BusinessVerificationResponse response = verificationService.submitVerification(member, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "사업자 인증 신청이 완료되었습니다. 관리자 승인 후 사업자 기능을 이용하실 수 있습니다.",
                    "data", response
            ));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("사업자 인증 신청 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "인증 신청 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 내 인증 상태 조회
     */
    @GetMapping("/my-status")
    public ResponseEntity<?> getMyStatus(Authentication authentication) {
        try {
            Member member = (Member) authentication.getPrincipal();
            var status = verificationService.getMyVerificationStatus(member);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "hasRequest", status.isPresent(),
                    "data", status.orElse(null)
            ));
        } catch (Exception e) {
            log.error("인증 상태 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "상태 조회 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 대기중인 인증 요청 목록 (관리자용)
     */
    @GetMapping("/admin/pending")
    public ResponseEntity<?> getPendingList(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Member admin = (Member) authentication.getPrincipal();
            Pageable pageable = PageRequest.of(page, size);
            Page<BusinessVerificationResponse> result = verificationService.getPendingVerifications(pageable);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result.getContent(),
                    "totalPages", result.getTotalPages(),
                    "totalElements", result.getTotalElements(),
                    "currentPage", result.getNumber()
            ));
        } catch (Exception e) {
            log.error("대기 목록 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "목록 조회 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 전체 인증 요청 목록 (관리자용)
     */
    @GetMapping("/admin/list")
    public ResponseEntity<?> getAllList(
            Authentication authentication,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<BusinessVerificationResponse> result;

            if (status != null && !status.isEmpty()) {
                VerificationStatus verificationStatus = VerificationStatus.valueOf(status.toUpperCase());
                result = verificationService.getVerificationsByStatus(verificationStatus, pageable);
            } else {
                result = verificationService.getAllVerifications(pageable);
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", result.getContent(),
                    "totalPages", result.getTotalPages(),
                    "totalElements", result.getTotalElements(),
                    "currentPage", result.getNumber()
            ));
        } catch (Exception e) {
            log.error("전체 목록 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "목록 조회 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 인증 요청 상세 조회 (관리자용)
     */
    @GetMapping("/admin/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Long id) {
        try {
            BusinessVerificationResponse response = verificationService.getVerificationDetail(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", response
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("상세 조회 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "상세 조회 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 사업자 인증 승인 (관리자용)
     */
    @PostMapping("/admin/{id}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            Member admin = (Member) authentication.getPrincipal();
            BusinessVerificationResponse response = verificationService.approveVerification(id, admin);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "사업자 인증이 승인되었습니다.",
                    "data", response
            ));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("승인 처리 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "승인 처리 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 사업자 인증 거절 (관리자용)
     */
    @PostMapping("/admin/{id}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        try {
            Member admin = (Member) authentication.getPrincipal();
            String reason = body.get("reason");
            BusinessVerificationResponse response = verificationService.rejectVerification(id, admin, reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "사업자 인증이 거절되었습니다.",
                    "data", response
            ));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("거절 처리 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "거절 처리 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 대기중인 인증 요청 수 (관리자용)
     */
    @GetMapping("/admin/pending-count")
    public ResponseEntity<?> getPendingCount() {
        try {
            long count = verificationService.getPendingCount();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", count
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "count", 0
            ));
        }
    }

    /**
     * 사업자 인증 신청 취소 (사용자)
     */
    @DeleteMapping("/cancel")
    public ResponseEntity<?> cancelVerification(Authentication authentication) {
        try {
            Member member = (Member) authentication.getPrincipal();
            verificationService.cancelVerification(member);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "사업자 인증 신청이 취소되었습니다."
            ));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("신청 취소 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "취소 처리 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 사업자 자격 포기 (사업자 본인)
     */
    @PostMapping("/resign")
    public ResponseEntity<?> resignBusinessRole(Authentication authentication) {
        try {
            Member member = (Member) authentication.getPrincipal();
            verificationService.resignBusinessRole(member);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "사업자 자격이 포기되었습니다. 일반 사용자로 전환됩니다."
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("사업자 자격 포기 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "처리 중 오류가 발생했습니다."
            ));
        }
    }

    /**
     * 사업자 자격 취소 (관리자)
     */
    @PostMapping("/admin/{memberId}/revoke")
    public ResponseEntity<?> revokeBusinessRole(
            @PathVariable Long memberId,
            Authentication authentication) {
        try {
            Member admin = (Member) authentication.getPrincipal();
            verificationService.revokeBusinessRole(memberId, admin);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "사업자 자격이 취소되었습니다."
            ));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("사업자 자격 취소 실패", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "처리 중 오류가 발생했습니다."
            ));
        }
    }
}

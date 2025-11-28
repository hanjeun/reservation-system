package ac.inhatc.reservation_system.customerservice.controller;

import ac.inhatc.reservation_system.config.util.SecurityUtil;
import ac.inhatc.reservation_system.customerservice.dto.InquiryDto;
import ac.inhatc.reservation_system.customerservice.service.InquiryService;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryApiController {

    private final InquiryService inquiryService;

    // 내 문의 목록 조회
    @GetMapping("/my")
    public ResponseEntity<Page<InquiryDto.InquiryResponse>> getMyInquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Page<InquiryDto.InquiryResponse> inquiries = inquiryService.getMyInquiries(memberId, page, size);
        return ResponseEntity.ok(inquiries);
    }

    // 전체 문의 목록 조회 (관리자용)
    @GetMapping("/admin/all")
    public ResponseEntity<Page<InquiryDto.InquiryResponse>> getAllInquiries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // 실제로는 관리자 권한 체크 필요
        Member currentMember = SecurityUtil.getCurrentMember();
        if (!currentMember.getRole().equals(Role.ADMIN)) {
            return ResponseEntity.status(403).build();
        }
        
        Page<InquiryDto.InquiryResponse> inquiries = inquiryService.getAllInquiries(page, size);
        return ResponseEntity.ok(inquiries);
    }

    // 문의 상세 조회
    @GetMapping("/{inquiryId}")
    public ResponseEntity<InquiryDto.InquiryResponse> getInquiry(@PathVariable Long inquiryId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        InquiryDto.InquiryResponse inquiry = inquiryService.getInquiry(inquiryId, memberId);
        return ResponseEntity.ok(inquiry);
    }

    // 문의 상세 조회 (관리자용)
    @GetMapping("/admin/{inquiryId}")
    public ResponseEntity<InquiryDto.InquiryResponse> getInquiryForAdmin(@PathVariable Long inquiryId) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (!currentMember.getRole().equals(Role.ADMIN)) {
            return ResponseEntity.status(403).build();
        }
        
        InquiryDto.InquiryResponse inquiry = inquiryService.getInquiryForAdmin(inquiryId);
        return ResponseEntity.ok(inquiry);
    }

    // 문의 작성
    @PostMapping
    public ResponseEntity<InquiryDto.InquiryResponse> createInquiry(@RequestBody InquiryDto.InquiryRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        InquiryDto.InquiryResponse inquiry = inquiryService.createInquiry(memberId, request);
        return ResponseEntity.ok(inquiry);
    }

    // 문의 삭제
    @DeleteMapping("/{inquiryId}")
    public ResponseEntity<Void> deleteInquiry(@PathVariable Long inquiryId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        inquiryService.deleteInquiry(inquiryId, memberId);
        return ResponseEntity.ok().build();
    }

    // 미답변 문의 개수
    @GetMapping("/pending-count")
    public ResponseEntity<Long> getPendingCount() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Long count = inquiryService.getPendingInquiryCount(memberId);
        return ResponseEntity.ok(count);
    }

    // 전체 미답변 문의 개수 (관리자용)
    @GetMapping("/admin/pending-count")
    public ResponseEntity<Long> getTotalPendingCount() {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (!currentMember.getRole().equals(Role.ADMIN)) {
            return ResponseEntity.status(403).build();
        }
        
        Long count = inquiryService.getTotalPendingInquiryCount();
        return ResponseEntity.ok(count);
    }

    // 답변 작성 (관리자용)
    @PostMapping("/{inquiryId}/answer")
    public ResponseEntity<InquiryDto.InquiryResponse> answerInquiry(
            @PathVariable Long inquiryId,
            @RequestBody InquiryDto.AnswerRequest request
    ) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (!currentMember.getRole().equals(Role.ADMIN)) {
            return ResponseEntity.status(403).build();
        }
        
        InquiryDto.InquiryResponse inquiry = inquiryService.answerInquiry(inquiryId, request);
        return ResponseEntity.ok(inquiry);
    }

    // 문의 삭제 (관리자용 - 답변 완료된 것도 삭제 가능)
    @DeleteMapping("/admin/{inquiryId}")
    public ResponseEntity<Void> deleteInquiryAsAdmin(@PathVariable Long inquiryId) {
        Member currentMember = SecurityUtil.getCurrentMember();
        if (!currentMember.getRole().equals(Role.ADMIN)) {
            return ResponseEntity.status(403).build();
        }

        inquiryService.deleteInquiryAsAdmin(inquiryId);
        return ResponseEntity.ok().build();
    }
}

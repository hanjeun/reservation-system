package ac.inhatc.reservation_system.customerservice.controller;

import ac.inhatc.reservation_system.config.util.SecurityUtil;
import ac.inhatc.reservation_system.customerservice.dto.NoticeDTO;
import ac.inhatc.reservation_system.customerservice.dto.NoticeRequestDTO;
import ac.inhatc.reservation_system.customerservice.service.NoticeService;
import ac.inhatc.reservation_system.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeApiController {

    private final NoticeService noticeService;

    // 모든 공지사항 조회
    @GetMapping
    public ResponseEntity<List<NoticeDTO>> getAllNotices() {
        List<NoticeDTO> notices = noticeService.getAllNotices();
        return ResponseEntity.ok(notices);
    }

    // 공지사항 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<NoticeDTO> getNotice(@PathVariable Long id) {
        NoticeDTO notice = noticeService.getNoticeById(id);
        return ResponseEntity.ok(notice);
    }

    // 공지사항 작성 (관리자만)
    @PostMapping
    public ResponseEntity<?> createNotice(@RequestBody NoticeRequestDTO requestDTO) {
        try {
            Member member = SecurityUtil.getCurrentMember();
            NoticeDTO notice = noticeService.createNotice(requestDTO, member.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(notice);
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("인증되지 않은")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "로그인이 필요합니다."));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 공지사항 수정 (관리자만)
    @PutMapping("/{id}")
    public ResponseEntity<?> updateNotice(
            @PathVariable Long id,
            @RequestBody NoticeRequestDTO requestDTO) {
        try {
            Member member = SecurityUtil.getCurrentMember();
            NoticeDTO notice = noticeService.updateNotice(id, requestDTO, member.getEmail());
            return ResponseEntity.ok(notice);
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("인증되지 않은")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "로그인이 필요합니다."));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 공지사항 삭제 (관리자만)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotice(@PathVariable Long id) {
        try {
            Member member = SecurityUtil.getCurrentMember();
            noticeService.deleteNotice(id, member.getEmail());
            return ResponseEntity.ok(Map.of("message", "공지사항이 삭제되었습니다."));
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("인증되지 않은")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "로그인이 필요합니다."));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

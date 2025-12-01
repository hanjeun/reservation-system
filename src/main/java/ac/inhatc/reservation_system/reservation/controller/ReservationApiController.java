package ac.inhatc.reservation_system.reservation.controller;

import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.reservation.dto.ReservationCreateRequest;
import ac.inhatc.reservation_system.reservation.dto.ReservationResponse;
import ac.inhatc.reservation_system.reservation.dto.ReservationUpdateRequest;
import ac.inhatc.reservation_system.reservation.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reservations")
public class ReservationApiController {

    private final ReservationService reservationService;

    /**
     * 예약 생성
     */
    @PostMapping
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody ReservationCreateRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");

        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("📝 예약 생성 요청: storeId={}, memberId={}", request.getStoreId(), member.getId());

        try {
            ReservationResponse reservation = reservationService.createReservation(request, member);
            return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
        } catch (IllegalArgumentException e) {
            log.error("❌ 예약 생성 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 내 예약 목록 조회
     */
    @GetMapping({"/my", "/my-reservations"})
    public ResponseEntity<List<ReservationResponse>> getMyReservations(
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("📋 내 예약 목록 조회: memberId={}", member.getId());
        List<ReservationResponse> reservations = reservationService.getMyReservations(member);
        return ResponseEntity.ok(reservations);
    }

    /**
     * 예약 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> getReservation(
            @PathVariable Long id,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            ReservationResponse reservation = reservationService.getReservation(id, member);
            return ResponseEntity.ok(reservation);
        } catch (IllegalArgumentException e) {
            log.error("❌ 예약 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 예약 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReservationResponse> updateReservation(
            @PathVariable Long id,
            @RequestBody ReservationUpdateRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("✏️ 예약 수정 요청: reservationId={}, memberId={}", id, member.getId());

        try {
            ReservationResponse reservation = reservationService.updateReservation(id, request, member);
            return ResponseEntity.ok(reservation);
        } catch (IllegalArgumentException e) {
            log.error("❌ 예약 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 예약 취소
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long id,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("❌ 예약 취소 요청: reservationId={}, memberId={}", id, member.getId());

        try {
            reservationService.cancelReservation(id, member);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("❌ 예약 취소 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 예약 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(
            @PathVariable Long id,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("🗑️ 예약 삭제 요청: reservationId={}, memberId={}", id, member.getId());

        try {
            reservationService.deleteReservation(id, member);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("❌ 예약 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ========== 사업자용 API ==========

    /**
     * 내 가게의 예약 목록 조회 (사업자용)
     */
    @GetMapping("/store-reservations")
    public ResponseEntity<List<ReservationResponse>> getStoreReservations(
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");

        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        // 사업자 권한 체크
        if (!member.isBusiness() && !member.isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        log.info("📋 가게 예약 목록 조회: memberId={}", member.getId());
        List<ReservationResponse> reservations = reservationService.getStoreReservations(member);
        return ResponseEntity.ok(reservations);
    }

    /**
     * 예약 승인 (사업자용)
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<Void> approveReservation(
            @PathVariable Long id,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");

        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        // 사업자 권한 체크
        if (!member.isBusiness() && !member.isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        log.info("✅ 예약 승인 요청: reservationId={}, memberId={}", id, member.getId());

        try {
            reservationService.approveReservation(id, member);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("❌ 예약 승인 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 예약 거절 (사업자용)
     */
    @PatchMapping("/{id}/reject")
    public ResponseEntity<Void> rejectReservation(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");

        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        // 사업자 권한 체크
        if (!member.isBusiness() && !member.isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        String rejectionReason = body != null ? body.get("rejectionReason") : null;

        log.info("❌ 예약 거절 요청: reservationId={}, memberId={}, reason={}", id, member.getId(), rejectionReason);

        try {
            reservationService.rejectReservation(id, member, rejectionReason);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("❌ 예약 거절 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 예약 취소 (사업자용 - 승인된 예약도 취소 가능)
     */
    @PatchMapping("/{id}/cancel-by-owner")
    public ResponseEntity<Void> cancelReservationByOwner(
            @PathVariable Long id,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");

        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        // 사업자 권한 체크
        if (!member.isBusiness() && !member.isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        log.info("❌ 사업자 예약 취소 요청: reservationId={}, memberId={}", id, member.getId());

        try {
            reservationService.cancelReservationByOwner(id, member);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("❌ 사업자 예약 취소 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 이용완료 처리 (사업자용)
     */
    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> completeReservation(
            @PathVariable Long id,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");

        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        // 사업자 권한 체크
        if (!member.isBusiness() && !member.isAdmin()) {
            return ResponseEntity.status(403).build();
        }

        log.info("✅ 이용완료 처리 요청: reservationId={}, memberId={}", id, member.getId());

        try {
            reservationService.completeReservation(id, member);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.error("❌ 이용완료 처리 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}

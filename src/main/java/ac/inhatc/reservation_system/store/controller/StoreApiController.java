package ac.inhatc.reservation_system.store.controller;

import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.store.dto.StoreCreateRequest;
import ac.inhatc.reservation_system.store.dto.StoreResponse;
import ac.inhatc.reservation_system.store.dto.StoreUpdateRequest;
import ac.inhatc.reservation_system.store.service.StoreService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/stores")
public class StoreApiController {

    private final StoreService storeService;

    @PostMapping
    public ResponseEntity<StoreResponse> createStore(
            @ModelAttribute StoreCreateRequest request,
            HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        if (!member.isBusiness() && !member.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        try {
            StoreResponse store = storeService.createStore(request, member);
            return ResponseEntity.ok(store);
        } catch (Exception e) {
            log.error("가게 등록 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/my")
    public ResponseEntity<List<StoreResponse>> getMyStores(HttpServletRequest httpRequest) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        
        if (!member.isBusiness() && !member.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        List<StoreResponse> stores = storeService.getMyStores(member);
        return ResponseEntity.ok(stores);
    }

    @GetMapping
    public ResponseEntity<List<StoreResponse>> getAllStores(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "rating") String sort
    ) {
        List<StoreResponse> stores = storeService.searchStores(keyword, sort);
        return ResponseEntity.ok(stores);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreResponse> getStore(@PathVariable Long id) {
        StoreResponse store = storeService.getStore(id);
        return ResponseEntity.ok(store);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoreResponse> updateStore(
            @PathVariable Long id,
            @ModelAttribute StoreUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("📝 가게 수정 요청 시작: storeId={}", id);

        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        if (member == null) {
            log.warn("❌ 가게 수정 실패: 인증되지 않은 사용자");
            return ResponseEntity.status(401).build();
        }

        log.info("👤 인증된 사용자: memberId={}, email={}", member.getId(), member.getEmail());

        if (!member.isBusiness() && !member.isAdmin()) {
            log.warn("❌ 가게 수정 실패: 권한 없음 (memberId={})", member.getId());
            return ResponseEntity.status(403).build();
        }

        try {
            log.info("📋 수정 요청 데이터: name={}, mainImage={}, detailImages={}",
                request.getName(),
                request.getMainImage() != null ? request.getMainImage().getOriginalFilename() : "없음",
                request.getDetailImages() != null ? request.getDetailImages().size() + "개" : "없음");

            StoreResponse store = storeService.updateStore(id, request, member);
            log.info("✅ 가게 수정 성공: storeId={}", id);
            return ResponseEntity.ok(store);
        } catch (IllegalArgumentException e) {
            log.error("❌ 가게 수정 실패 (IllegalArgumentException): storeId={}, message={}", id, e.getMessage());
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            log.error("❌ 가게 수정 실패 (Exception): storeId={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStore(
            @PathVariable Long id,
            HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        
        if (!member.isBusiness() && !member.isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        storeService.deleteStore(id, member);
        return ResponseEntity.noContent().build();
    }
}

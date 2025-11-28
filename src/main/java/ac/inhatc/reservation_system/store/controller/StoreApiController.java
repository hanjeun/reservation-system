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

        // 🔒 권한 체크: BUSINESS 또는 ADMIN만 가게 등록 가능
        if (!member.isBusiness() && !member.isAdmin()) {
            log.warn("❌ 권한 없음: 일반 사용자가 가게 등록 시도 - memberId={}", member.getId());
            return ResponseEntity.status(403).build(); // 403 Forbidden
        }

        log.info("✅ 가게 등록 API: memberId={}, storeName={}", member.getId(), request.getName());
        
        try {
            StoreResponse store = storeService.createStore(request, member);
            return ResponseEntity.ok(store);
        } catch (Exception e) {
            log.error("❌ 가게 등록 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/my")
    public ResponseEntity<List<StoreResponse>> getMyStores(HttpServletRequest httpRequest) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        
        // 🔒 권한 체크: BUSINESS 또는 ADMIN만 내 가게 조회 가능
        if (!member.isBusiness() && !member.isAdmin()) {
            log.warn("❌ 권한 없음: 일반 사용자가 가게 목록 조회 시도 - memberId={}", member.getId());
            return ResponseEntity.status(403).build(); // 403 Forbidden
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
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        
        // 🔒 권한 체크: BUSINESS 또는 ADMIN만 가게 수정 가능
        if (!member.isBusiness() && !member.isAdmin()) {
            log.warn("❌ 권한 없음: 일반 사용자가 가게 수정 시도 - memberId={}, storeId={}", member.getId(), id);
            return ResponseEntity.status(403).build();
        }
        
        StoreResponse store = storeService.updateStore(id, request, member);
        return ResponseEntity.ok(store);
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
        
        // 🔒 권한 체크: BUSINESS 또는 ADMIN만 가게 삭제 가능
        if (!member.isBusiness() && !member.isAdmin()) {
            log.warn("❌ 권한 없음: 일반 사용자가 가게 삭제 시도 - memberId={}, storeId={}", member.getId(), id);
            return ResponseEntity.status(403).build();
        }
        
        storeService.deleteStore(id, member);
        return ResponseEntity.noContent().build();
    }
}

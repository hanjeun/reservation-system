package ac.inhatc.reservation_system.promotion.controller;

import ac.inhatc.reservation_system.config.util.SecurityUtil;
import ac.inhatc.reservation_system.promotion.dto.PromotionDto;
import ac.inhatc.reservation_system.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionApiController {

    private final PromotionService promotionService;

    // 전체 홍보글 목록 조회 (일반 사용자에게는 "추천 가게")
    @GetMapping
    public ResponseEntity<Page<PromotionDto.PromotionResponse>> getAllPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "latest") String sortBy
    ) {
        Page<PromotionDto.PromotionResponse> promotions = promotionService.getAllPromotions(page, size, sortBy);
        return ResponseEntity.ok(promotions);
    }

    // 홍보글 상세 조회
    @GetMapping("/{promotionId}")
    public ResponseEntity<PromotionDto.PromotionResponse> getPromotion(@PathVariable Long promotionId) {
        PromotionDto.PromotionResponse promotion = promotionService.getPromotion(promotionId);
        return ResponseEntity.ok(promotion);
    }

    // 내가 등록한 가게 목록 조회 (사업자/관리자용)
    @GetMapping("/my-stores")
    public ResponseEntity<List<PromotionDto.StoreSimpleResponse>> getMyStores() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<PromotionDto.StoreSimpleResponse> stores = promotionService.getMyStores(memberId);
        return ResponseEntity.ok(stores);
    }

    // 홍보글 작성 (사업자/관리자용)
    @PostMapping
    public ResponseEntity<PromotionDto.PromotionResponse> createPromotion(
            @RequestBody PromotionDto.PromotionRequest request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        PromotionDto.PromotionResponse promotion = promotionService.createPromotion(memberId, request);
        return ResponseEntity.ok(promotion);
    }

    // 홍보글 수정
    @PutMapping("/{promotionId}")
    public ResponseEntity<PromotionDto.PromotionResponse> updatePromotion(
            @PathVariable Long promotionId,
            @RequestBody PromotionDto.PromotionRequest request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        PromotionDto.PromotionResponse promotion = promotionService.updatePromotion(promotionId, memberId, request);
        return ResponseEntity.ok(promotion);
    }

    // 홍보글 삭제
    @DeleteMapping("/{promotionId}")
    public ResponseEntity<Void> deletePromotion(@PathVariable Long promotionId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        promotionService.deletePromotion(promotionId, memberId);
        return ResponseEntity.ok().build();
    }

    // 내가 작성한 홍보글 목록 조회
    @GetMapping("/my")
    public ResponseEntity<Page<PromotionDto.PromotionResponse>> getMyPromotions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Page<PromotionDto.PromotionResponse> promotions = promotionService.getMyPromotions(memberId, page, size);
        return ResponseEntity.ok(promotions);
    }
}

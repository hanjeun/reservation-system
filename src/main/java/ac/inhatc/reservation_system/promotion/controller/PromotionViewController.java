package ac.inhatc.reservation_system.promotion.controller;

import ac.inhatc.reservation_system.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/promotion")
@RequiredArgsConstructor
public class PromotionViewController {

    private final PromotionService promotionService;

    /**
     * 홍보 목록 페이지
     */
    @GetMapping
    public String promotionListPage() {
        return "store-promotion";
    }

    /**
     * 홍보 상세 페이지
     */
    @GetMapping("/{promotionId}")
    public String promotionDetailPage(@PathVariable Long promotionId, Model model) {
        model.addAttribute("promotionId", promotionId);
        return "store-promotion-detail";
    }
}

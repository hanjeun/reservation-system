package ac.inhatc.reservation_system.store.controller;

import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.store.dto.StoreCreateRequest;
import ac.inhatc.reservation_system.store.dto.StoreResponse;
import ac.inhatc.reservation_system.store.service.StoreService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/store")
public class StoreController {

    private final StoreService storeService;

    /**
     * 가게 등록 폼 페이지
     */
    @GetMapping("/register")
    public String showStoreRegisterForm(HttpServletRequest httpRequest, RedirectAttributes redirectAttributes) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");

        if (member == null) {
            redirectAttributes.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/user/login";
        }

        if (!member.isBusiness() && !member.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "가게 등록은 사업자 회원만 가능합니다.");
            return "redirect:/";
        }

        return "store-register";
    }

    @PostMapping("/register")
    public String registerStore(
            @ModelAttribute StoreCreateRequest request,
            HttpServletRequest httpRequest,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Member member = (Member) httpRequest.getAttribute("authenticatedUser");

            if (member == null) {
                redirectAttributes.addFlashAttribute("error", "로그인이 필요합니다.");
                return "redirect:/user/login";
            }

            if (!member.isBusiness() && !member.isAdmin()) {
                redirectAttributes.addFlashAttribute("error", "가게 등록은 사업자 회원만 가능합니다.");
                return "redirect:/";
            }

            StoreResponse store = storeService.createStore(request, member);

            redirectAttributes.addFlashAttribute("message", "가게가 성공적으로 등록되었습니다!");
            redirectAttributes.addFlashAttribute("storeId", store.getId());

            return "redirect:/store/stores";

        } catch (Exception e) {
            log.error("가게 등록 실패", e);
            redirectAttributes.addFlashAttribute("error", "가게 등록에 실패했습니다: " + e.getMessage());
            return "redirect:/store/register";
        }
    }

    @GetMapping("/promotion")
    public String showStorePromotionForm(HttpServletRequest httpRequest, RedirectAttributes redirectAttributes) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");

        if (member == null) {
            redirectAttributes.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/user/login";
        }

        return "store-promotion";
    }

    /**
     * 가게 수정 폼 페이지
     */
    @GetMapping("/edit/{id}")
    public String showStoreEditForm(@PathVariable Long id, HttpServletRequest httpRequest, RedirectAttributes redirectAttributes) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");

        if (member == null) {
            redirectAttributes.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/user/login";
        }

        if (!member.isBusiness() && !member.isAdmin()) {
            redirectAttributes.addFlashAttribute("error", "가게 수정은 사업자 회원만 가능합니다.");
            return "redirect:/";
        }

        return "store-edit";
    }

    /**
     * 가게 검색 페이지
     */
    @GetMapping({"/list", "/stores"})
    public String storeList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "rating") String sort,
            Model model
    ) {
        List<StoreResponse> stores = storeService.searchStores(keyword, sort);

        model.addAttribute("stores", stores);
        model.addAttribute("keyword", keyword != null ? keyword : "");
        model.addAttribute("currentSort", sort);
        model.addAttribute("storeCount", stores.size());

        return "store-list";
    }

    /**
     * 가게 상세 페이지
     */
    @GetMapping("/{id}")
    public String storeDetail(@PathVariable Long id, Model model) {
        try {
            StoreResponse store = storeService.getStore(id);
            model.addAttribute("store", store);
            return "store-detail";
        } catch (Exception e) {
            log.error("가게 상세 정보 로드 실패: storeId={}", id, e);
            return "redirect:/store/stores";
        }
    }
}

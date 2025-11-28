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
        // 🔒 권한 체크: BUSINESS 또는 ADMIN만 접근 가능
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        
        if (member == null) {
            redirectAttributes.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/user/login";
        }
        
        if (!member.isBusiness() && !member.isAdmin()) {
            log.warn("❌ 권한 없음: 일반 사용자가 가게 등록 페이지 접근 시도 - memberId={}", member.getId());
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
            // JWT 필터에서 설정한 인증된 사용자 가져오기
            Member member = (Member) httpRequest.getAttribute("authenticatedUser");
            
            if (member == null) {
                redirectAttributes.addFlashAttribute("error", "로그인이 필요합니다.");
                return "redirect:/user/login";
            }
            
            // 🔒 권한 체크: BUSINESS 또는 ADMIN만 가게 등록 가능
            if (!member.isBusiness() && !member.isAdmin()) {
                log.warn("❌ 권한 없음: 일반 사용자가 가게 등록 시도 - memberId={}", member.getId());
                redirectAttributes.addFlashAttribute("error", "가게 등록은 사업자 회원만 가능합니다.");
                return "redirect:/";
            }
            
            log.info("✅ 가게 등록 요청: {} (사용자: {})", request.getName(), member.getEmail());
            
            StoreResponse store = storeService.createStore(request, member);
            
            redirectAttributes.addFlashAttribute("message", "가게가 성공적으로 등록되었습니다!");
            redirectAttributes.addFlashAttribute("storeId", store.getId());
            
            return "redirect:/store/stores";
            
        } catch (Exception e) {
            log.error("❌ 가게 등록 실패", e);
            redirectAttributes.addFlashAttribute("error", "가게 등록에 실패했습니다: " + e.getMessage());
            return "redirect:/store/register";
        }
    }

    @GetMapping("/promotion")
    public String showStorePromotionForm(HttpServletRequest httpRequest, RedirectAttributes redirectAttributes) {
        // 로그인 여부만 체크 (모든 로그인 사용자 접근 가능)
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        
        if (member == null) {
            redirectAttributes.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/user/login";
        }
        
        // 일반 사용자는 "추천 가게", 사업자/관리자는 "홍보하기"
        log.info("✅ 홍보/추천 페이지 접근 - memberId={}, role={}", member.getId(), member.getRole());
        
        return "store-promotion";
    }

    /**
     * 가게 수정 폼 페이지
     */
    @GetMapping("/edit/{id}")
    public String showStoreEditForm(@PathVariable Long id, HttpServletRequest httpRequest, RedirectAttributes redirectAttributes) {
        // 🔒 권한 체크: BUSINESS 또는 ADMIN만 접근 가능
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        
        if (member == null) {
            redirectAttributes.addFlashAttribute("error", "로그인이 필요합니다.");
            return "redirect:/user/login";
        }
        
        if (!member.isBusiness() && !member.isAdmin()) {
            log.warn("❌ 권한 없음: 일반 사용자가 가게 수정 페이지 접근 시도 - memberId={}, storeId={}", member.getId(), id);
            redirectAttributes.addFlashAttribute("error", "가게 수정은 사업자 회원만 가능합니다.");
            return "redirect:/";
        }
        
        return "store-edit";
    }

    /**
     * 가게 검색 페이지
     * /store/list, /store/stores 두 경로 모두 지원
     */
    @GetMapping({"/list", "/stores"})
    public String storeList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "rating") String sort,
            Model model
    ) {
        log.info("🔍 가게 검색 페이지 요청");
        log.info("  - keyword: {}", keyword);
        log.info("  - sort: {}", sort);
        
        List<StoreResponse> stores = storeService.searchStores(keyword, sort);
        
        log.info("✅ 가게 검색 결과: {}개", stores.size());
        if (stores.size() > 0) {
            stores.forEach(store -> 
                log.info("  - 가게: id={}, name={}, mainImage={}", 
                    store.getId(), store.getName(), store.getMainImageUrl())
            );
        }
        
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
    public String storeDetail(
            @PathVariable Long id,
            Model model
    ) {
        log.info("🏪 가게 상세 페이지 요청: storeId={}", id);
        
        try {
            StoreResponse store = storeService.getStore(id);
            
            model.addAttribute("store", store);
            
            log.info("✅ 가게 상세 정보 로드 완료: {}", store.getName());
            
            return "store-detail";
        } catch (Exception e) {
            log.error("❌ 가게 상세 정보 로드 실패", e);
            return "redirect:/store/stores";
        }
    }
}

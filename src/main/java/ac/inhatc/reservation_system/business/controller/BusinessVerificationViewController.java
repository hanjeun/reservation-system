package ac.inhatc.reservation_system.business.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class BusinessVerificationViewController {

    /**
     * 사업자 인증 관리 페이지 (관리자용)
     */
    @GetMapping("/business-verification")
    public String businessVerificationPage() {
        return "admin/business-verification";
    }
}

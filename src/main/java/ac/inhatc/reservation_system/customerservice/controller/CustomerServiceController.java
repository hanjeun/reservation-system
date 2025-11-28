package ac.inhatc.reservation_system.customerservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/customer-service")
@Controller
public class CustomerServiceController {

    @GetMapping("/inquiry")
    public String showCsInquiryForm() {
        return "customer-service-inquiry.html";
    }

    @GetMapping("/notice")
    public String showCsNoticeForm() {
        return "customer-service-notice.html";
    }

    @GetMapping("/policy")
    public String showCsPolicyForm() {
        return "customer-service-policy.html";
    }

    @GetMapping("/admin/inquiry")
    public String showAdminInquiry() {
        return "admin-inquiry.html";
    }
}

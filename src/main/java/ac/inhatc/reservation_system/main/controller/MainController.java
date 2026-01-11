package ac.inhatc.reservation_system.main.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String index() {
        return "main-page.html";
    }

    @GetMapping("/main")
    public String mainForm() {
        return "main-page.html";
    }

    /**
     * 인앱 브라우저 안내 페이지
     */
    @GetMapping("/browser-guide")
    public String browserGuide() {
        return "browser-guide";
    }
}


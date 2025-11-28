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
}



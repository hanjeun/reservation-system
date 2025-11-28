package ac.inhatc.reservation_system.community.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/community")
public class CommunityController {

    @GetMapping
    public String showCommunity() {
        return "community";
    }
    
    @GetMapping("/detail")
    public String showCommunityDetail() {
        return "community-detail";
    }
}

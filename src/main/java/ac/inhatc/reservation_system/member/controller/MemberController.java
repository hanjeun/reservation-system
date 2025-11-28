package ac.inhatc.reservation_system.member.controller;

import ac.inhatc.reservation_system.config.jwt.JwtProperties;
import ac.inhatc.reservation_system.config.jwt.TokenProvider;
import ac.inhatc.reservation_system.member.dto.MemberDto;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@RequestMapping("/user")
@RequiredArgsConstructor
@Controller
public class MemberController {

    private final MemberService memberService;
    private final TokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("memberDto", new MemberDto());
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(
            @Valid @ModelAttribute("memberDto") MemberDto memberDto,
            BindingResult bindingResult,
            HttpServletResponse response
    ) {
        if (bindingResult.hasErrors()) {
            return "signup";
        }

        if (!memberDto.getPassword().equals(memberDto.getPasswordConfirm())) {
            bindingResult.rejectValue("passwordConfirm", "passwordInCorrect", "비밀번호가 일치하지 않습니다.");
            return "signup";
        }

        try {
            Long memberId = memberService.save(memberDto);
            Member member = memberService.findById(memberId);

            // 회원가입 후 자동 로그인 - JWT 토큰 발급
            String accessToken = tokenProvider.generateAccessToken(member);
            String refreshToken = tokenProvider.generateRefreshToken(member);

            addTokenCookie(response, "access_token", accessToken,
                (int) jwtProperties.getAccessTokenExpiration().toSeconds());
            addTokenCookie(response, "refresh_token", refreshToken,
                (int) jwtProperties.getRefreshTokenExpiration().toSeconds());

            log.info("✅ 회원가입 및 자동 로그인 완료: {}", member.getEmail());

            return "redirect:/";

        } catch (IllegalArgumentException e) {
            bindingResult.reject("signupFailed", e.getMessage());
            return "signup";
        }
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @GetMapping("/mypage")
    public String mypage() {
        return "mypage";
    }

    @GetMapping("/edit")
    public String editForm() {
        return "member-edit";
    }

    private void addTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(false); // JavaScript에서 접근 가능하도록 설정
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }
}
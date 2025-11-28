package ac.inhatc.reservation_system.member.controller;

import ac.inhatc.reservation_system.member.dto.MemberResponse;
import ac.inhatc.reservation_system.member.dto.MemberUpdateRequest;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/member")
public class MemberApiController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getCurrentMember(HttpServletRequest httpRequest) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        
        if (member == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(MemberResponse.from(member));
    }
    
    @PutMapping("/update")
    public ResponseEntity<MemberResponse> updateMember(
            @RequestBody MemberUpdateRequest request,
            HttpServletRequest httpRequest
    ) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        
        MemberResponse updated = memberService.updateMember(member.getId(), request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteMember(HttpServletRequest httpRequest) {
        Member member = (Member) httpRequest.getAttribute("authenticatedUser");
        
        if (member == null) {
            return ResponseEntity.status(401).build();
        }
        
        memberService.deleteMember(member.getId());
        return ResponseEntity.ok().build();
    }
}

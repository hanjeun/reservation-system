package ac.inhatc.reservation_system.config.util;

import ac.inhatc.reservation_system.member.entity.Member;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

public class SecurityUtil {

    /**
     * 현재 인증된 사용자의 Member ID를 반환합니다.
     * @return 현재 사용자의 ID
     * @throws IllegalStateException 인증되지 않은 경우
     */
    public static Long getCurrentMemberId() {
        HttpServletRequest request = getCurrentHttpRequest();
        Member member = (Member) request.getAttribute("authenticatedUser");
        
        if (member == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }
        
        return member.getId();
    }

    /**
     * 현재 인증된 Member 객체를 반환합니다.
     * @return 현재 사용자의 Member 객체
     * @throws IllegalStateException 인증되지 않은 경우
     */
    public static Member getCurrentMember() {
        HttpServletRequest request = getCurrentHttpRequest();
        Member member = (Member) request.getAttribute("authenticatedUser");
        
        if (member == null) {
            throw new IllegalStateException("인증되지 않은 사용자입니다.");
        }
        
        return member;
    }

    /**
     * 현재 HTTP 요청을 반환합니다.
     * @return HttpServletRequest
     */
    private static HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = 
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        
        if (attributes == null) {
            throw new IllegalStateException("현재 요청 컨텍스트를 찾을 수 없습니다.");
        }
        
        return attributes.getRequest();
    }
}

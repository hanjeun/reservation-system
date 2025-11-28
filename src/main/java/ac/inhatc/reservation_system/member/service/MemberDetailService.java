package ac.inhatc.reservation_system.member.service;

// ❌❌❌ 이 파일은 사용되지 않습니다 ❌❌❌
// 
// 이전 방식: formLogin + UserDetailsService
// 현재 방식: JWT 인증 (UserDetailsService 불필요)
//
// JWT 인증에서는:
// 1. TokenProvider가 JWT에서 직접 사용자 ID를 추출
// 2. MemberRepository를 직접 사용하여 Member 조회
// 3. UserDetailsService가 필요 없음
//
// 정리 이유:
// 1. JWT 인증 방식으로 변경됨
// 2. Spring Security의 formLogin을 사용하지 않음
// 3. UserDetailsService는 formLogin에서만 필요
//
// 이 파일은 안전하게 삭제할 수 있습니다.

/*
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberDetailService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(email + "을(를) 찾을 수 없습니다."));
        
        // Member를 UserDetails로 변환하여 반환
        return org.springframework.security.core.userdetails.User
                .withUsername(member.getEmail())
                .password(member.getPassword())
                .authorities("ROLE_USER")
                .build();
    }
}
*/

package ac.inhatc.reservation_system.member.service;


import ac.inhatc.reservation_system.config.jwt.entity.RefreshToken;
import ac.inhatc.reservation_system.config.jwt.repository.RefreshTokenRepository;
import ac.inhatc.reservation_system.member.dto.MemberDto;
import ac.inhatc.reservation_system.member.dto.MemberResponse;
import ac.inhatc.reservation_system.member.dto.MemberUpdateRequest;
import ac.inhatc.reservation_system.member.entity.Member;
import ac.inhatc.reservation_system.member.entity.Role;
import ac.inhatc.reservation_system.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder bCryptPasswordEncoder;

    public Long save(MemberDto memberDto) {
        if (memberRepository.findByEmail(memberDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        return memberRepository.save(Member.builder()
                .name(memberDto.getName())
                .email(memberDto.getEmail())
                .password(bCryptPasswordEncoder.encode(memberDto.getPassword()))
                .role(memberDto.getRole()) // 권한 추가
                .build()).getId();
    }

    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
    }

    public Member findByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다."));
    }
    
    @Transactional
    public MemberResponse updateMember(Long memberId, MemberUpdateRequest request) {
        log.info("✏️ 회원 정보 수정: memberId={}", memberId);
        
        Member member = findById(memberId);
        
        // 이름 수정
        if (request.getName() != null && !request.getName().isEmpty()) {
            member.setName(request.getName());
        }
        
        // 이메일 수정 (중복 체크)
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!member.getEmail().equals(request.getEmail())) {
                if (memberRepository.findByEmail(request.getEmail()).isPresent()) {
                    throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                }
                member.setEmail(request.getEmail());
            }
        }
        
        // 비밀번호 수정 (입력된 경우에만)
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            if (request.getPassword().length() < 8) {
                throw new IllegalArgumentException("비밀번호는 8자 이상이어야 합니다.");
            }
            if (!request.getPassword().equals(request.getPasswordConfirm())) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }
            member.setPassword(bCryptPasswordEncoder.encode(request.getPassword()));
        }
        
        // 권한 수정
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try {
                Role newRole = Role.valueOf(request.getRole().toUpperCase());
                member.setRole(newRole);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("유효하지 않은 권한입니다: " + request.getRole());
            }
        }
        
        Member updated = memberRepository.save(member);
        log.info("✅ 회원 정보 수정 완료: memberId={}", memberId);
        
        return MemberResponse.from(updated);
    }

    @Transactional
    public void deleteMember(Long memberId) {
        // 1. 모든 리프레쉬 토큰 삭제 (멀티 디바이스 지원)
        List<RefreshToken> tokens = refreshTokenRepository.findByUserId(memberId);
        refreshTokenRepository.deleteAll(tokens);
        log.info("🗑️ 사용자의 모든 Refresh Token 삭제: userId={}, count={}", memberId, tokens.size());

        // 2. 회원 정보 삭제
        memberRepository.deleteById(memberId);
    }
}

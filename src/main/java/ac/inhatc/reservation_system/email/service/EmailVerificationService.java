package ac.inhatc.reservation_system.email.service;

import ac.inhatc.reservation_system.email.entity.EmailVerification;
import ac.inhatc.reservation_system.email.repository.EmailVerificationRepository;
import ac.inhatc.reservation_system.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository verificationRepository;
    private final MemberRepository memberRepository;
    private final EmailService emailService;

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_MINUTES = 5;

    /**
     * 인증 코드 생성 및 이메일 발송
     */
    @Transactional
    public void sendVerificationCode(String email) {
        // 이미 가입된 이메일인지 확인
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 기존 인증 정보 삭제
        verificationRepository.deleteByEmail(email);

        // 새 인증 코드 생성
        String code = generateVerificationCode();

        // DB에 저장
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .expiresAt(LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES))
                .verified(false)
                .build();

        verificationRepository.save(verification);

        // 이메일 발송
        emailService.sendVerificationEmail(email, code);

        log.info("📧 인증 코드 발송: email={}", email);
    }

    /**
     * 인증 코드 검증
     */
    @Transactional
    public boolean verifyCode(String email, String code) {
        EmailVerification verification = verificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("인증 요청을 찾을 수 없습니다. 인증 코드를 다시 요청해주세요."));

        if (verification.isExpired()) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다. 다시 요청해주세요.");
        }

        if (!verification.getVerificationCode().equals(code)) {
            throw new IllegalArgumentException("인증 코드가 일치하지 않습니다.");
        }

        verification.setVerified(true);
        verificationRepository.save(verification);

        log.info("✅ 이메일 인증 완료: email={}", email);
        return true;
    }

    /**
     * 이메일 인증 여부 확인
     */
    public boolean isEmailVerified(String email) {
        return verificationRepository.findByEmailAndVerifiedTrue(email).isPresent();
    }

    /**
     * 6자리 숫자 인증 코드 생성
     */
    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}

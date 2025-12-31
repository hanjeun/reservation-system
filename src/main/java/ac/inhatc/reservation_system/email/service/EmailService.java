package ac.inhatc.reservation_system.email.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.username}")
    private String fromEmail;

    /**
     * 인증 코드 이메일 발송
     */
    @Async
    public void sendVerificationEmail(String toEmail, String verificationCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[통합예약시스템] 이메일 인증 코드");
            helper.setText(buildVerificationEmailContent(verificationCode), true);

            mailSender.send(message);
            log.info("✅ 인증 이메일 발송 완료: {}", toEmail);

        } catch (MessagingException e) {
            log.error("❌ 이메일 발송 실패: {}", e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다.", e);
        }
    }

    /**
     * 이메일 본문 HTML 생성
     */
    private String buildVerificationEmailContent(String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif; }
                    .container { max-width: 600px; margin: 0 auto; padding: 40px 20px; }
                    .header { text-align: center; margin-bottom: 40px; }
                    .logo { font-size: 28px; font-weight: bold; color: #667eea; }
                    .content { background: #f8f9fa; border-radius: 16px; padding: 40px; text-align: center; }
                    .title { font-size: 24px; color: #333; margin-bottom: 20px; }
                    .code-box { 
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        font-size: 36px;
                        font-weight: bold;
                        letter-spacing: 8px;
                        padding: 20px 40px;
                        border-radius: 12px;
                        display: inline-block;
                        margin: 20px 0;
                    }
                    .description { color: #666; font-size: 14px; line-height: 1.6; margin-top: 20px; }
                    .warning { color: #e74c3c; font-size: 13px; margin-top: 16px; }
                    .footer { text-align: center; margin-top: 40px; color: #999; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="logo">🎯 통합예약시스템</div>
                    </div>
                    <div class="content">
                        <h1 class="title">이메일 인증 코드</h1>
                        <p>아래 인증 코드를 입력해주세요.</p>
                        <div class="code-box">%s</div>
                        <p class="description">
                            이 코드는 <strong>5분간</strong> 유효합니다.<br>
                            본인이 요청하지 않은 경우 이 메일을 무시하세요.
                        </p>
                        <p class="warning">⚠️ 이 코드를 다른 사람과 공유하지 마세요.</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 통합예약시스템. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(code);
    }
}

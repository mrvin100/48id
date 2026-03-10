package io.k48.fortyeightid.auth.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
class EmailService {

    private final JavaMailSender mailSender;

    @Value("${fortyeightid.mail.from:no-reply@48id.k48.io}")
    private String fromAddress;

    @Value("${fortyeightid.mail.reset-password-url:http://localhost:3000/reset-password}")
    private String resetPasswordBaseUrl;

    @Async
    void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
        try {
            var message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("K48 ID — Password Reset Request");
            message.setText("""
                    Hello %s,

                    An administrator has requested a password reset for your K48 ID account.

                    Click the link below to reset your password (valid for 1 hour):

                    %s?token=%s

                    If you did not request this, please contact K48 administration immediately.

                    — K48 ID Team
                    """.formatted(userName, resetPasswordBaseUrl, resetToken));
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MailException ex) {
            log.error("Failed to send password reset email to {}: {}", toEmail, ex.getMessage(), ex);
        }
    }
}

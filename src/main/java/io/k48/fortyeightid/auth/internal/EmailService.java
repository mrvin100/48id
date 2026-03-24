package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.auth.EmailPort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.mail.internet.InternetAddress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

@Slf4j
@Service
@RequiredArgsConstructor
class EmailService implements EmailPort {

    private static final String ACTIVATION_EMAIL_TEMPLATE = "templates/activation-email.html";
    private static final String OPERATOR_INVITE_EMAIL_TEMPLATE = "templates/operator-invite-email.html";

    private final JavaMailSender mailSender;

    @Value("${fortyeightid.mail.from:no-reply@48id.k48.io}")
    private String fromAddress;

    @Value("${fortyeightid.mail.activation-url:http://localhost:3000/activate-account}")
    private String activationBaseUrl;

    @Value("${fortyeightid.mail.reset-password-url:http://localhost:3000/reset-password}")
    private String resetPasswordBaseUrl;

    @Value("${fortyeightid.mail.login-url:http://localhost:3000/login}")
    private String loginUrl;

    @Value("${fortyeightid.mail.operator-invite-url:http://localhost:3000/accept-operator-invite}")
    private String operatorInviteBaseUrl;

    @Override
    @Async
    public void sendActivationEmail(String toEmail, String userName, String matricule, String temporaryPassword, String activationToken) {
        try {
            String htmlContent = loadTemplate(ACTIVATION_EMAIL_TEMPLATE)
                    .replace("{{userName}}", escapeHtml(userName))
                    .replace("{{matricule}}", escapeHtml(matricule))
                    .replace("{{temporaryPassword}}", escapeHtml(temporaryPassword))
                    .replace("{{loginUrl}}", escapeHtml(loginUrl))
                    .replace("{{activationUrl}}", escapeHtml(activationBaseUrl + "?token=" + activationToken));

            MimeMessagePreparator preparator = msg -> {
                msg.setFrom(new InternetAddress(fromAddress));
                msg.setRecipients(jakarta.mail.Message.RecipientType.TO, InternetAddress.parse(toEmail));
                msg.setSubject("K48 ID — Activate your account");
                msg.setText(htmlContent, StandardCharsets.UTF_8.name(), "html");
            };
            mailSender.send(preparator);
            log.info("Activation email sent to {}", toEmail);
        } catch (MailException ex) {
            log.error("Failed to send activation email to {}: {}", toEmail, ex.getMessage(), ex);
        } catch (IOException ex) {
            log.error("Failed to load activation email template: {}", ex.getMessage(), ex);
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String toEmail, String userName, String resetToken) {
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

    @Override
    @Async
    public void sendOperatorInviteEmail(String toEmail, String userName, String inviteToken) {
        try {
            String htmlContent = loadTemplate(OPERATOR_INVITE_EMAIL_TEMPLATE)
                    .replace("{{userName}}", escapeHtml(userName))
                    .replace("{{inviteUrl}}", escapeHtml(operatorInviteBaseUrl + "?token=" + inviteToken));

            MimeMessagePreparator preparator = msg -> {
                msg.setFrom(new InternetAddress(fromAddress));
                msg.setRecipients(jakarta.mail.Message.RecipientType.TO, InternetAddress.parse(toEmail));
                msg.setSubject("K48 ID — You have been invited as an Operator");
                msg.setText(htmlContent, StandardCharsets.UTF_8.name(), "html");
            };
            mailSender.send(preparator);
            log.info("Operator invite email sent to {}", toEmail);
        } catch (MailException ex) {
            log.error("Failed to send operator invite email to {}: {}", toEmail, ex.getMessage(), ex);
        } catch (IOException ex) {
            log.error("Failed to load operator invite email template: {}", ex.getMessage(), ex);
        }
    }

    private String loadTemplate(String path) throws IOException {
        var resource = new ClassPathResource(path);
        return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}

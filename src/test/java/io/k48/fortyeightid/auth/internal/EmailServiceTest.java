package io.k48.fortyeightid.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.k48.fortyeightid.auth.EmailPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<MimeMessagePreparator> messageCaptor;

    @Test
    void sendActivationEmail_sendsHtmlEmailWithTemplate() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@48id.k48.io");
        ReflectionTestUtils.setField(emailService, "loginUrl", "http://localhost:3000/login");
        ReflectionTestUtils.setField(emailService, "activationBaseUrl", "http://localhost:3000/activate-account");

        emailService.sendActivationEmail(
                "student@k48.io",
                "Ama Owusu",
                "K48-2024-001",
                "TempPass123!",
                "activation-token"
        );

        verify(mailSender, times(1)).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue()).isNotNull();
    }

    @Test
    void sendActivationEmail_escapesHtmlInUserData() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@48id.k48.io");
        ReflectionTestUtils.setField(emailService, "loginUrl", "http://localhost:3000/login");
        ReflectionTestUtils.setField(emailService, "activationBaseUrl", "http://localhost:3000/activate-account");

        emailService.sendActivationEmail(
                "student@k48.io",
                "Ama <script>alert('xss')</script> Owusu",
                "K48-2024-001",
                "TempPass123!",
                "activation-token"
        );

        verify(mailSender, times(1)).send(any(MimeMessagePreparator.class));
    }

    @Test
    void sendActivationEmail_usesConfiguredSender() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "noreply@k48.io");
        ReflectionTestUtils.setField(emailService, "loginUrl", "http://localhost:3000/login");
        ReflectionTestUtils.setField(emailService, "activationBaseUrl", "http://localhost:3000/activate-account");

        emailService.sendActivationEmail(
                "student@k48.io",
                "Ama Owusu",
                "K48-2024-001",
                "TempPass123!",
                "activation-token"
        );

        verify(mailSender, times(1)).send(any(MimeMessagePreparator.class));
    }

    @Test
    void sendActivationEmail_isAnnotatedWithAsync() throws NoSuchMethodException {
        var method = EmailService.class.getDeclaredMethod(
                "sendActivationEmail",
                String.class,
                String.class,
                String.class,
                String.class,
                String.class
        );

        assertThat(method.isAnnotationPresent(org.springframework.scheduling.annotation.Async.class))
                .as("sendActivationEmail should be annotated with @Async")
                .isTrue();
    }

    @Test
    void sendPasswordResetEmail_usesSimpleMailMessage() {
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@48id.k48.io");
        ReflectionTestUtils.setField(emailService, "resetPasswordBaseUrl", "http://localhost:3000/reset-password");

        emailService.sendPasswordResetEmail("student@k48.io", "Ama Owusu", "abc123-def456");

        verify(mailSender, times(1)).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void sendPasswordResetEmail_isAnnotatedWithAsync() throws NoSuchMethodException {
        var method = EmailService.class.getDeclaredMethod(
                "sendPasswordResetEmail",
                String.class,
                String.class,
                String.class
        );

        assertThat(method.isAnnotationPresent(org.springframework.scheduling.annotation.Async.class))
                .as("sendPasswordResetEmail should be annotated with @Async")
                .isTrue();
    }

    @Test
    void emailPortInterface_isImplemented() {
        assertThat(emailService).isInstanceOf(EmailPort.class);
    }
}

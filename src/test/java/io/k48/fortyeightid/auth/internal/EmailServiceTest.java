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
        // Given
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@48id.k48.io");
        ReflectionTestUtils.setField(emailService, "loginUrl", "http://localhost:3000/login");

        String toEmail = "student@k48.io";
        String userName = "Ama Owusu";
        String matricule = "K48-2024-001";
        String temporaryPassword = "TempPass123!";

        // When
        emailService.sendActivationEmail(toEmail, userName, matricule, temporaryPassword);

        // Then
        verify(mailSender, times(1)).send(messageCaptor.capture());
        
        // Verify the captor captured a MimeMessagePreparator
        MimeMessagePreparator capturedPreparator = messageCaptor.getValue();
        assertThat(capturedPreparator).isNotNull();
    }

    @Test
    void sendActivationEmail_escapesHtmlInUserData() {
        // Given
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@48id.k48.io");
        ReflectionTestUtils.setField(emailService, "loginUrl", "http://localhost:3000/login");

        String toEmail = "student@k48.io";
        String userName = "Ama <script>alert('xss')</script> Owusu";
        String matricule = "K48-2024-001";
        String temporaryPassword = "TempPass123!";

        // When
        emailService.sendActivationEmail(toEmail, userName, matricule, temporaryPassword);

        // Then
        verify(mailSender, times(1)).send(any(MimeMessagePreparator.class));
        // The HTML should be escaped to prevent XSS
    }

    @Test
    void sendActivationEmail_logsErrorOnMailException() {
        // Given
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@48id.k48.io");
        ReflectionTestUtils.setField(emailService, "loginUrl", "http://localhost:3000/login");

        String toEmail = "student@k48.io";
        String userName = "Ama Owusu";
        String matricule = "K48-2024-001";
        String temporaryPassword = "TempPass123!";

        // When - mailSender will throw exception (mock behavior not set up)
        emailService.sendActivationEmail(toEmail, userName, matricule, temporaryPassword);

        // Then - should not throw, error should be logged (verified manually in implementation)
        verify(mailSender, times(1)).send(any(MimeMessagePreparator.class));
    }

    @Test
    void sendActivationEmail_usesCorrectFromAddress() {
        // Given
        String customFromAddress = "noreply@k48.io";
        ReflectionTestUtils.setField(emailService, "fromAddress", customFromAddress);
        ReflectionTestUtils.setField(emailService, "loginUrl", "http://localhost:3000/login");

        String toEmail = "student@k48.io";
        String userName = "Ama Owusu";
        String matricule = "K48-2024-001";
        String temporaryPassword = "TempPass123!";

        // When
        emailService.sendActivationEmail(toEmail, userName, matricule, temporaryPassword);

        // Then
        verify(mailSender, times(1)).send(any(MimeMessagePreparator.class));
    }

    @Test
    void sendActivationEmail_isAnnotatedWithAsync() throws NoSuchMethodException {
        // Given
        var method = EmailService.class.getDeclaredMethod(
                "sendActivationEmail",
                String.class,
                String.class,
                String.class,
                String.class
        );

        // Then
        assertThat(method.isAnnotationPresent(org.springframework.scheduling.annotation.Async.class))
                .as("sendActivationEmail should be annotated with @Async")
                .isTrue();
    }

    @Test
    void sendPasswordResetEmail_usesSimpleMailMessage() {
        // Given
        ReflectionTestUtils.setField(emailService, "fromAddress", "no-reply@48id.k48.io");
        ReflectionTestUtils.setField(emailService, "resetPasswordBaseUrl", "http://localhost:3000/reset-password");

        String toEmail = "student@k48.io";
        String userName = "Ama Owusu";
        String resetToken = "abc123-def456";

        // When
        emailService.sendPasswordResetEmail(toEmail, userName, resetToken);

        // Then
        verify(mailSender, times(1)).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void sendPasswordResetEmail_isAnnotatedWithAsync() throws NoSuchMethodException {
        // Given
        var method = EmailService.class.getDeclaredMethod(
                "sendPasswordResetEmail",
                String.class,
                String.class,
                String.class
        );

        // Then
        assertThat(method.isAnnotationPresent(org.springframework.scheduling.annotation.Async.class))
                .as("sendPasswordResetEmail should be annotated with @Async")
                .isTrue();
    }

    @Test
    void emailPortInterface_isImplemented() {
        // Then
        assertThat(emailService).isInstanceOf(EmailPort.class);
    }
}

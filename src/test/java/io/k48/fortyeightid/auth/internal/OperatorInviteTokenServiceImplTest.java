package io.k48.fortyeightid.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.shared.exception.ResetTokenExpiredException;
import io.k48.fortyeightid.shared.exception.ResetTokenInvalidException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatorInviteTokenServiceImplTest {

    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @InjectMocks private OperatorInviteTokenServiceImpl service;

    @Test
    void createInviteToken_deletesExistingAndSavesNew() {
        var userId = UUID.randomUUID();
        when(passwordResetTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var token = service.createInviteToken(userId, 86400);

        assertThat(token).isNotBlank();
        verify(passwordResetTokenRepository).deleteAllByUserIdAndPurpose(userId, ResetTokenPurpose.OPERATOR_INVITE);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    void validateAndConsumeInviteToken_validToken_returnsUserId() {
        var userId = UUID.randomUUID();
        var rawToken = UUID.randomUUID().toString();
        var token = buildToken(rawToken, userId, false, false);

        when(passwordResetTokenRepository.findByToken(rawToken)).thenReturn(Optional.of(token));
        when(passwordResetTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.validateAndConsumeInviteToken(rawToken);

        assertThat(result).isEqualTo(userId);
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void validateAndConsumeInviteToken_invalidToken_throws() {
        when(passwordResetTokenRepository.findByToken("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateAndConsumeInviteToken("bad"))
                .isInstanceOf(ResetTokenInvalidException.class);
    }

    @Test
    void validateAndConsumeInviteToken_expiredToken_throws() {
        var rawToken = UUID.randomUUID().toString();
        var token = buildToken(rawToken, UUID.randomUUID(), true, false);

        when(passwordResetTokenRepository.findByToken(rawToken)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validateAndConsumeInviteToken(rawToken))
                .isInstanceOf(ResetTokenExpiredException.class);
    }

    @Test
    void validateAndConsumeInviteToken_alreadyUsedToken_throws() {
        var rawToken = UUID.randomUUID().toString();
        var token = buildToken(rawToken, UUID.randomUUID(), false, true);

        when(passwordResetTokenRepository.findByToken(rawToken)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validateAndConsumeInviteToken(rawToken))
                .isInstanceOf(ResetTokenInvalidException.class);
    }

    private PasswordResetToken buildToken(String rawToken, UUID userId, boolean expired, boolean used) {
        return PasswordResetToken.builder()
                .userId(userId)
                .token(rawToken)
                .purpose(ResetTokenPurpose.OPERATOR_INVITE)
                .expiresAt(expired ? Instant.now().minusSeconds(3600) : Instant.now().plusSeconds(3600))
                .used(used)
                .build();
    }
}

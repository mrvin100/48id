package io.k48.fortyeightid.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.k48.fortyeightid.auth.internal.OperatorInviteToken;
import io.k48.fortyeightid.auth.internal.OperatorInviteTokenRepository;
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

    @Mock private OperatorInviteTokenRepository repository;
    @InjectMocks private OperatorInviteTokenServiceImpl service;

    @Test
    void createInviteToken_deletesExistingAndSavesNew() {
        var userId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        var token = service.createInviteToken(userId, accountId, 86400);

        assertThat(token).isNotBlank();
        verify(repository).deleteAllByUserId(userId);
        verify(repository).save(any(OperatorInviteToken.class));
    }

    @Test
    void validateAndConsumeInviteToken_validToken_returnsPayload() {
        var userId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var rawToken = UUID.randomUUID().toString();
        var token = buildToken(rawToken, userId, accountId, false, false);

        when(repository.findByToken(rawToken)).thenReturn(Optional.of(token));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = service.validateAndConsumeInviteToken(rawToken);

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.accountId()).isEqualTo(accountId);
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void validateAndConsumeInviteToken_invalidToken_throws() {
        when(repository.findByToken("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateAndConsumeInviteToken("bad"))
                .isInstanceOf(ResetTokenInvalidException.class);
    }

    @Test
    void validateAndConsumeInviteToken_expiredToken_throws() {
        var rawToken = UUID.randomUUID().toString();
        var token = buildToken(rawToken, UUID.randomUUID(), UUID.randomUUID(), true, false);

        when(repository.findByToken(rawToken)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validateAndConsumeInviteToken(rawToken))
                .isInstanceOf(ResetTokenExpiredException.class);
    }

    @Test
    void validateAndConsumeInviteToken_alreadyUsedToken_throws() {
        var rawToken = UUID.randomUUID().toString();
        var token = buildToken(rawToken, UUID.randomUUID(), UUID.randomUUID(), false, true);

        when(repository.findByToken(rawToken)).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.validateAndConsumeInviteToken(rawToken))
                .isInstanceOf(ResetTokenInvalidException.class);
    }

    private OperatorInviteToken buildToken(String rawToken, UUID userId, UUID accountId, boolean expired, boolean used) {
        return OperatorInviteToken.builder()
                .token(rawToken)
                .userId(userId)
                .accountId(accountId)
                .expiresAt(expired ? Instant.now().minusSeconds(3600) : Instant.now().plusSeconds(3600))
                .used(used)
                .build();
    }
}

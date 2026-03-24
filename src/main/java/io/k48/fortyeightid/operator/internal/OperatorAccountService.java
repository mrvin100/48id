package io.k48.fortyeightid.operator.internal;

import io.k48.fortyeightid.audit.AuditService;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.operator.OperatorAccountPort;
import io.k48.fortyeightid.operator.internal.OperatorMembership.MemberRole;
import io.k48.fortyeightid.operator.internal.OperatorMembership.MembershipStatus;
import io.k48.fortyeightid.shared.exception.OperatorAccountNameTakenException;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class OperatorAccountService implements OperatorAccountPort {

    private static final Logger log = LoggerFactory.getLogger(OperatorAccountService.class);

    private final OperatorAccountRepository accountRepository;
    private final OperatorMembershipRepository membershipRepository;
    private final UserQueryService userQueryService;
    private final AuditService auditService;

    @Override
    @Transactional
    public OperatorAccountCreated createAccount(CreateOperatorAccountCommand command) {
        if (accountRepository.existsByName(command.name())) {
            throw new OperatorAccountNameTakenException(command.name());
        }

        var admin = userQueryService.findById(command.adminId())
            .orElseThrow(() -> new UserNotFoundException("Admin not found: " + command.adminId()));

        OperatorAccount account;
        try {
            account = accountRepository.save(OperatorAccount.builder()
                .name(command.name())
                .description(command.description())
                .createdBy(command.adminId())
                .build());
        } catch (DataIntegrityViolationException ex) {
            // Race condition: another transaction inserted the same name concurrently
            throw new OperatorAccountNameTakenException(command.name());
        }

        var membership = membershipRepository.save(OperatorMembership.builder()
            .accountId(account.getId())
            .userId(command.adminId())
            .memberRole(MemberRole.OWNER)
            .status(MembershipStatus.ACTIVE)
            .acceptedAt(Instant.now())
            .build());

        log.info("OperatorAccount created: id={}, name={}, owner={}", account.getId(), account.getName(), command.adminId());

        auditService.log(command.adminId(), "OPERATOR_ACCOUNT_CREATED", Map.of(
            "accountId", account.getId().toString(),
            "accountName", account.getName(),
            "createdBy", command.adminId().toString()
        ));

        return new OperatorAccountCreated(
            account.getId(),
            account.getName(),
            account.getDescription(),
            account.getCreatedBy(),
            account.getCreatedAt() != null ? account.getCreatedAt() : Instant.now(),
            membership.getId(),
            command.adminId(),
            admin.getMatricule(),
            admin.getName()
        );
    }
}

package io.k48.fortyeightid.identity;

import io.k48.fortyeightid.identity.internal.CreateUserRequest;
import io.k48.fortyeightid.identity.internal.UserService;
import io.k48.fortyeightid.shared.exception.DuplicateEmailException;
import io.k48.fortyeightid.shared.exception.DuplicateMatriculeException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Public facade for cross-module user provisioning (e.g. CSV import).
 * Delegates to the package-private {@link UserService}.
 */
@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final UserService userService;

    /**
     * Creates a new user account with PENDING_ACTIVATION status.
     *
     * @throws DuplicateMatriculeException if matricule already exists
     * @throws DuplicateEmailException     if email already exists
     */
    public User createUser(String matricule, String email, String name,
                           String phone, String batch, String specialization,
                           String rawPassword) {
        var request = new CreateUserRequest(
                matricule, email, name, phone, batch, specialization, rawPassword);
        return userService.createUser(request);
    }
}

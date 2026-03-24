package io.k48.fortyeightid.identity;

/**
 * Public port for cross-module user provisioning (e.g. CSV import).
 */
public interface UserProvisioningPort {

    /**
     * Creates a new user account with PENDING_ACTIVATION status.
     *
     * @throws io.k48.fortyeightid.shared.exception.DuplicateMatriculeException if matricule already exists
     * @throws io.k48.fortyeightid.shared.exception.DuplicateEmailException     if email already exists
     */
    User createUser(String matricule, String email, String name,
                    String phone, String batch, String specialization,
                    String rawPassword);
}

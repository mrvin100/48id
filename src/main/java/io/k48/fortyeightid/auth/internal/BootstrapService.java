package io.k48.fortyeightid.auth.internal;

import io.k48.fortyeightid.identity.Role;
import io.k48.fortyeightid.identity.RoleQueryService;
import io.k48.fortyeightid.identity.User;
import io.k48.fortyeightid.identity.UserQueryService;
import io.k48.fortyeightid.identity.UserStatus;
import io.k48.fortyeightid.shared.exception.DuplicateEmailException;
import io.k48.fortyeightid.shared.exception.DuplicateMatriculeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Service for bootstrapping the first admin user in the system.
 * This service can only create an admin when no other admin users exist.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class BootstrapService {

    private final UserQueryService userQueryService;
    private final RoleQueryService roleQueryService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates the first admin user in the system.
     * This operation can only be performed when no admin users exist.
     *
     * @param request Bootstrap request with admin user details
     * @return Bootstrap response with created user information
     * @throws IllegalStateException if admin users already exist
     * @throws DuplicateMatriculeException if matricule already exists
     * @throws DuplicateEmailException if email already exists
     */
    @Transactional
    public BootstrapResponse createFirstAdmin(BootstrapRequest request) {
        log.info("Attempting to create first admin user with matricule: {}", request.matricule());

        // Check if any admin users already exist
        var adminRole = roleQueryService.getByName("ADMIN");

        long adminCount = userQueryService.countByRolesContaining(adminRole);
        if (adminCount > 0) {
            log.warn("Bootstrap attempt rejected - {} admin users already exist", adminCount);
            throw new IllegalStateException("Cannot create admin user - admin users already exist in the system");
        }

        // Validate uniqueness
        if (userQueryService.existsByMatricule(request.matricule())) {
            throw new DuplicateMatriculeException("Matricule already exists: " + request.matricule());
        }
        if (userQueryService.existsByEmail(request.email())) {
            throw new DuplicateEmailException("Email already exists: " + request.email());
        }

        // Create the admin user
        var user = User.builder()
                .matricule(request.matricule())
                .email(request.email())
                .name(request.name())
                .phone(request.phone())
                .batch(request.batch())
                .specialization(request.specialization())
                .passwordHash(passwordEncoder.encode(request.password()))
                .status(UserStatus.ACTIVE) // Admin is immediately active
                .roles(Set.of(adminRole))
                .profileCompleted(true) // Admin profile is complete
                .build();

        var savedUser = userQueryService.save(user);
        
        log.info("Successfully created first admin user: {} ({})", savedUser.getMatricule(), savedUser.getId());

        return new BootstrapResponse(
                "First admin user created successfully. Bootstrap endpoint is now disabled.",
                savedUser.getId().toString(),
                savedUser.getMatricule(),
                savedUser.getEmail()
        );
    }

    /**
     * Checks if the bootstrap operation is available.
     * Bootstrap is only available when no admin users exist.
     *
     * @return true if bootstrap is available, false otherwise
     */
    public boolean isBootstrapAvailable() {
        var adminRole = roleQueryService.findByName("ADMIN");
        if (adminRole.isEmpty()) {
            return false; // No admin role means system is not properly initialized
        }
        
        long adminCount = userQueryService.countByRolesContaining(adminRole.get());
        return adminCount == 0;
    }
}
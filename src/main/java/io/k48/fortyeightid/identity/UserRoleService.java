package io.k48.fortyeightid.identity;

import io.k48.fortyeightid.identity.internal.RoleRepository;
import io.k48.fortyeightid.identity.internal.UserRepository;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Replaces all roles with a single new role.
     * OPERATOR cannot be assigned this way — it is earned through OperatorAccount ownership.
     */
    @Transactional
    public User changeRole(UUID userId, String newRoleName) {
        if ("OPERATOR".equals(newRoleName)) {
            throw new IllegalArgumentException(
                    "OPERATOR role cannot be assigned directly. Use the operator account flow.");
        }
        var user = getUser(userId);
        var role = getRole(newRoleName);
        user.getRoles().clear();
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    /**
     * Adds a role without removing existing roles.
     */
    @Transactional
    public User addRole(UUID userId, String roleName) {
        var user = getUser(userId);
        var role = getRole(roleName);
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    /**
     * Removes a single role. If the user would have no roles left, keeps STUDENT as fallback.
     */
    @Transactional
    public User removeRole(UUID userId, String roleName) {
        var user = getUser(userId);
        user.getRoles().removeIf(r -> r.getName().equals(roleName));
        if (user.getRoles().isEmpty()) {
            user.getRoles().add(getRole("STUDENT"));
        }
        return userRepository.save(user);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
    }

    private Role getRole(String name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + name));
    }
}

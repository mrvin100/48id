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

    @Transactional
    public User changeRole(UUID userId, String newRoleName) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        var role = roleRepository.findByName(newRoleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + newRoleName));

        user.getRoles().clear();
        user.getRoles().add(role);
        return userRepository.save(user);
    }
}

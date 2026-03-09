package io.k48.fortyeightid.identity;

import io.k48.fortyeightid.identity.internal.UserRepository;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserStatusService {

    private final UserRepository userRepository;

    @Transactional
    public User changeStatus(UUID userId, UserStatus newStatus) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));
        user.setStatus(newStatus);
        return userRepository.save(user);
    }
}

package io.k48.fortyeightid.identity;

import io.k48.fortyeightid.identity.internal.UserRepository;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;

    public Optional<User> findByMatricule(String matricule) {
        return userRepository.findByMatricule(matricule);
    }

    public User getByMatricule(String matricule) {
        return userRepository.findByMatricule(matricule)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + matricule));
    }
}

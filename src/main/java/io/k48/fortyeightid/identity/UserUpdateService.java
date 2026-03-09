package io.k48.fortyeightid.identity;

import io.k48.fortyeightid.identity.internal.UserRepository;
import io.k48.fortyeightid.shared.exception.DuplicateEmailException;
import io.k48.fortyeightid.shared.exception.UserNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserUpdateService {

    private final UserRepository userRepository;

    @Transactional
    public UpdateResult updateProfile(UUID userId, String email, String name, String phone,
                                      String batch, String specialization) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        var changedFields = new ArrayList<String>();

        if (email != null && !email.equals(user.getEmail())) {
            userRepository.findByEmail(email).ifPresent(existing -> {
                if (!existing.getId().equals(userId)) {
                    throw new DuplicateEmailException("Email already in use: " + email);
                }
            });
            user.setEmail(email);
            changedFields.add("email");
        }
        if (name != null && !name.equals(user.getName())) {
            user.setName(name);
            changedFields.add("name");
        }
        if (phone != null && !phone.equals(user.getPhone())) {
            user.setPhone(phone);
            changedFields.add("phone");
        }
        if (batch != null && !batch.equals(user.getBatch())) {
            user.setBatch(batch);
            changedFields.add("batch");
        }
        if (specialization != null && !specialization.equals(user.getSpecialization())) {
            user.setSpecialization(specialization);
            changedFields.add("specialization");
        }

        var saved = userRepository.save(user);
        return new UpdateResult(saved, changedFields);
    }

    public record UpdateResult(User user, List<String> changedFields) {}
}

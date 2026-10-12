package com.satyam.splitly.user;

import com.satyam.splitly.common.exception.ConflictException;
import com.satyam.splitly.common.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Transactional
    public User register(String name, String rawEmail, String passwordHash) {
        String email = normalise(rawEmail);

        // Checked here so the caller gets a clear 409 rather than a raw constraint
        // violation. The unique index on users.email is still the real guard - two
        // concurrent registrations can both pass this check, and the database is what
        // stops the second one.
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("A user with email " + email + " already exists");
        }
        User user = new User(name.trim(), email);
        user.setPasswordHash(passwordHash);
        return userRepository.save(user);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("User", id));
    }

    public List<User> searchByEmail(String fragment) {
        return userRepository.findByEmailContainingIgnoreCase(normalise(fragment));
    }

    /** Emails are matched case-insensitively, so they are stored in one canonical form. */
    private String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}

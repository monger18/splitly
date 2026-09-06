package com.satyam.splitly.user;

import com.satyam.splitly.common.exception.ConflictException;
import com.satyam.splitly.common.exception.NotFoundException;
import com.satyam.splitly.user.dto.RegisterUserRequest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for users.
 *
 * <p>The service layer is where a transaction begins and ends. Note the class-level
 * {@code readOnly = true} with writes opting out individually: a read-only transaction
 * lets Hibernate skip dirty-checking, and it makes an accidental write in a query path
 * fail loudly instead of silently persisting.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    /**
     * Constructor injection, not {@code @Autowired} on a field. The dependency is final
     * so the object cannot exist in a half-built state, and the class can be constructed
     * in a plain unit test with {@code new UserService(mockRepo)} - no Spring context.
     * Spring injects a single constructor automatically; the annotation is not needed.
     */
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public User register(RegisterUserRequest request) {
        String email = normalise(request.email());

        // Checked here so the caller gets a clear 409 rather than a raw constraint
        // violation. The unique index on users.email is still the real guard - two
        // concurrent registrations can both pass this check, and the database is what
        // stops the second one.
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("A user with email " + email + " already exists");
        }
        return userRepository.save(new User(request.name().trim(), email));
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

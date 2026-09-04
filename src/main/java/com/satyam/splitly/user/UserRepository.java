package com.satyam.splitly.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data writes the implementation at startup from the method names, so
 * findByEmail becomes "select ... from users where email = ?". There is no class
 * to write and nothing to register - extending the interface is enough.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByEmailContainingIgnoreCase(String fragment);
}

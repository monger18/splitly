package com.satyam.splitly.auth;

import com.satyam.splitly.auth.dto.LoginRequest;
import com.satyam.splitly.auth.dto.RefreshRequest;
import com.satyam.splitly.auth.dto.RegisterRequest;
import com.satyam.splitly.auth.dto.TokenResponse;
import com.satyam.splitly.common.exception.UnauthorizedException;
import com.satyam.splitly.user.User;
import com.satyam.splitly.user.UserRepository;
import com.satyam.splitly.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, login and refresh.
 *
 * <p>Passwords are verified here directly against the {@link PasswordEncoder} rather than
 * through a {@code UserDetailsService} and {@code AuthenticationManager}. For a single
 * email-and-password login that indirection adds three more moving parts and hides the one
 * line that matters. A larger application with several authentication methods - OAuth,
 * LDAP, an API key - is where that machinery starts earning its place.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, UserService userService,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Registers a user and logs them straight in, so a client needs one call, not two. */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        User user = userService.register(request.name(), request.email(),
                passwordEncoder.encode(request.password()));
        return issueTokens(user.getId());
    }

    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElse(null);

        // One message and one status for every failure - unknown email, wrong password, or
        // an account with no password set. Saying "no such user" would let anyone test
        // which email addresses are registered, one request at a time.
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return issueTokens(user.getId());
    }

    /**
     * Exchanges a refresh token for a new pair.
     *
     * <p>Stateless: the token is trusted on its signature alone, with nothing stored
     * server-side. The honest limitation is that it therefore <strong>cannot be
     * revoked</strong> before it expires - logging out only discards the client's copy. A
     * production system would keep a store of refresh-token ids so logout and "sign out
     * everywhere" could invalidate them. That is a deliberate scope choice, not an
     * oversight.
     */
    public TokenResponse refresh(RefreshRequest request) {
        Long userId = jwtService.parseUserId(request.refreshToken(), TokenType.REFRESH);

        // The token may outlive the account it names, so the user is re-checked rather
        // than trusted from the claims alone.
        if (!userRepository.existsById(userId)) {
            throw new UnauthorizedException("Invalid token");
        }
        return issueTokens(userId);
    }

    private TokenResponse issueTokens(Long userId) {
        return TokenResponse.of(
                jwtService.createAccessToken(userId),
                jwtService.createRefreshToken(userId),
                jwtService.accessTokenTtl().toSeconds());
    }
}

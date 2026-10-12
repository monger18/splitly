package com.satyam.splitly.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * The security rules for the whole API.
 *
 * <p>{@code @EnableMethodSecurity} switches on {@code @PreAuthorize}, used by
 * {@link com.satyam.splitly.group.GroupAccess} for the admin-only operations.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * BCrypt, with the library's default cost factor.
     *
     * <p>BCrypt is deliberately slow and salts every hash itself, so two people with the
     * same password get different hashes and a stolen table cannot be attacked with a
     * precomputed rainbow table. A general-purpose hash like SHA-256 is the wrong tool
     * here precisely because it is fast.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthFilter jwtAuthFilter,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                // No cookies and no server-side session, so there is no session for an
                // attacker's site to ride. CSRF protection guards cookie-based auth; with
                // a bearer token it adds nothing and blocks every non-GET request.
                .csrf(csrf -> csrf.disable())

                // Never create a session. Each request must carry its own token, which is
                // what lets the application scale horizontally with no shared session store.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Everything not named above needs a token. Default-deny, so a new
                        // endpoint is protected the moment it is written rather than when
                        // somebody remembers to add it here.
                        .anyRequest().authenticated())

                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                // Runs before the form-login filter so a valid token authenticates the
                // request before anything tries to redirect it to a login page.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

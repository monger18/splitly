package com.satyam.splitly.auth;

import com.satyam.splitly.common.exception.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the bearer token on each request and populates the security context.
 *
 * <p>Extends {@code OncePerRequestFilter} rather than implementing {@code Filter}: a
 * request can be dispatched more than once inside the container - forwards, error pages,
 * async dispatches - and the plain interface would run the token check again each time.
 *
 * <p>A missing or bad token is <strong>not</strong> rejected here. The filter simply
 * leaves the context empty and lets the chain continue, so the authorization rules in
 * {@link SecurityConfig} decide. That keeps one place answering "is this endpoint
 * public?" instead of two that can disagree, and lets
 * {@link RestAuthenticationEntryPoint} produce the same error body as the rest of the API.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);

        if (header != null && header.startsWith(PREFIX)) {
            String token = header.substring(PREFIX.length()).trim();
            try {
                // Only ACCESS tokens authenticate a request. A refresh token presented
                // here is rejected, which is the whole point of the typ claim.
                Long userId = jwtService.parseUserId(token, TokenType.ACCESS);

                var authentication = new UsernamePasswordAuthenticationToken(
                        new AuthenticatedUser(userId, null),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (UnauthorizedException ex) {
                // Leave the context empty and carry on. Clearing it explicitly matters if
                // anything earlier in the chain set an authentication we should not trust.
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}

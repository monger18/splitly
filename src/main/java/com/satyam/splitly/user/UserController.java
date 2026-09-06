package com.satyam.splitly.user;

import com.satyam.splitly.common.auth.CurrentUserId;
import com.satyam.splitly.user.dto.RegisterUserRequest;
import com.satyam.splitly.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP layer for users.
 *
 * <p>Controllers here do three things only: bind and validate input, call one service
 * method, and map the result to a response DTO. No business rules, no repository calls,
 * no try/catch - exceptions unwind to
 * {@link com.satyam.splitly.common.exception.GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 201 Created, because a POST that creates a resource should not answer 200.
     * {@code @Valid} is what actually activates the annotations on the request record.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a user")
    public UserResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return UserResponse.from(userService.register(request));
    }

    @GetMapping("/me")
    @Operation(summary = "The calling user", description = "Identified by the X-User-Id header until phase 4 replaces it with a JWT.")
    public UserResponse me(@CurrentUserId Long userId) {
        return UserResponse.from(userService.getById(userId));
    }

    @GetMapping("/search")
    @Operation(summary = "Find users by part of their email", description = "Used to pick people to add to a group.")
    public List<UserResponse> search(@RequestParam @NotBlank String email) {
        return userService.searchByEmail(email).stream().map(UserResponse::from).toList();
    }
}

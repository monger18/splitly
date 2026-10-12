package com.satyam.splitly.user;

import com.satyam.splitly.common.auth.CurrentUserId;
import com.satyam.splitly.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


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
    @GetMapping("/me")
    @Operation(summary = "The calling user", description = "Identified by the subject of the request's access token.")
    public UserResponse me(@CurrentUserId Long userId) {
        return UserResponse.from(userService.getById(userId));
    }

    @GetMapping("/search")
    @Operation(summary = "Find users by part of their email", description = "Used to pick people to add to a group.")
    public List<UserResponse> search(@RequestParam @NotBlank String email) {
        return userService.searchByEmail(email).stream().map(UserResponse::from).toList();
    }
}

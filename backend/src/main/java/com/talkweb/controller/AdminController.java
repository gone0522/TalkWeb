package com.talkweb.controller;

import com.talkweb.dto.ApiResponse;
import com.talkweb.dto.CreateUserRequest;
import com.talkweb.dto.UserDto;
import com.talkweb.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @PostMapping("/users")
    public ApiResponse<Map<String, String>> createUser(@Valid @RequestBody CreateUserRequest request) {
        String oneTimePassword = userService.createUser(request);
        return ApiResponse.success("用戶建立成功", Map.of(
                "username", request.getUsername(),
                "oneTimePassword", oneTimePassword
        ));
    }

    @GetMapping("/users")
    public ApiResponse<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsersForAdmin();
        return ApiResponse.success(users);
    }

    @PutMapping("/users/{id}/status")
    public ApiResponse<Void> updateUserStatus(@PathVariable("id") Long id,
                                              @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "ACTIVE");
        userService.updateUserStatus(id, status);
        return ApiResponse.success("使用者狀態更新成功", null);
    }

    @PostMapping("/users/{id}/reset-password")
    public ApiResponse<Map<String, String>> resetPassword(@PathVariable("id") Long id) {
        String newOneTimePassword = userService.resetPassword(id);
        return ApiResponse.success("密碼已重置為新的一次性密碼", Map.of(
                "newOneTimePassword", newOneTimePassword
        ));
    }
}

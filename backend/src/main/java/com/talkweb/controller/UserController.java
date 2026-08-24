package com.talkweb.controller;

import com.talkweb.dto.ApiResponse;
import com.talkweb.dto.UpdateProfileRequest;
import com.talkweb.dto.UserDto;
import com.talkweb.security.UserPrincipal;
import com.talkweb.service.AvatarService;
import com.talkweb.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AvatarService avatarService;

    @GetMapping("/users/me")
    public ApiResponse<UserDto> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        UserDto userDto = userService.getCurrentUser(principal.getId());
        return ApiResponse.success(userDto);
    }

    @PutMapping("/users/me")
    public ApiResponse<UserDto> updateMe(@AuthenticationPrincipal UserPrincipal principal,
                                         @Valid @RequestBody UpdateProfileRequest request) {
        UserDto updated = userService.updateProfile(principal.getId(), request);
        return ApiResponse.success("個人資料更新成功", updated);
    }

    @PostMapping(value = "/users/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> uploadAvatar(@AuthenticationPrincipal UserPrincipal principal,
                                          @RequestParam("file") MultipartFile file) throws IOException {
        avatarService.uploadAvatar(principal.getId(), file);
        return ApiResponse.success("頭像上傳成功", null);
    }

    @GetMapping("/users/{id}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable("id") Long id) {
        byte[] avatarData = avatarService.getAvatarData(id);
        if (avatarData == null || avatarData.length == 0) {
            return ResponseEntity.notFound().build();
        }

        String mimeType = avatarService.getAvatarMimeType(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mimeType));
        headers.setCacheControl("public, max-age=3600");

        return new ResponseEntity<>(avatarData, headers, HttpStatus.OK);
    }

    @GetMapping("/contacts")
    public ApiResponse<List<UserDto>> getContacts(@AuthenticationPrincipal UserPrincipal principal,
                                                  @RequestParam(value = "search", required = false) String search) {
        List<UserDto> contacts = (search != null && !search.trim().isEmpty())
                ? userService.searchUsers(search, principal.getId())
                : userService.getAllActiveContacts(principal.getId());
        return ApiResponse.success(contacts);
    }

    @GetMapping("/contacts/check")
    public ApiResponse<UserDto> checkContact(@AuthenticationPrincipal UserPrincipal principal,
                                             @RequestParam("username") String username) {
        UserDto user = userService.checkUserByUsername(username, principal.getId());
        return ApiResponse.success("找到使用者", user);
    }

    @PostMapping("/contacts/add")
    public ApiResponse<UserDto> addContact(@AuthenticationPrincipal UserPrincipal principal,
                                           @Valid @RequestBody com.talkweb.dto.AddFriendRequest request) {
        UserDto user = userService.addFriend(principal.getId(), request.getUsername());
        return ApiResponse.success("成功新增好友", user);
    }
}

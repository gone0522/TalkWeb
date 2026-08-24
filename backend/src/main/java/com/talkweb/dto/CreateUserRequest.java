package com.talkweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    @NotBlank(message = "使用者名稱不得為空")
    @Pattern(regexp = "^[a-zA-Z0-9._-]{3,30}$", message = "使用者名稱需為 3~30 碼英數字或 . _ -")
    private String username;

    @NotBlank(message = "暱稱不得為空")
    private String nickname;

    private boolean admin;

    private Integer defaultAvatarIcon;
}

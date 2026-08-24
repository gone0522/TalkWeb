package com.talkweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "帳號不得為空")
    @Pattern(regexp = "^[a-zA-Z0-9._-]{3,30}$", message = "帳號需為 3~30 碼英數或 . _ -")
    private String username;

    @NotBlank(message = "暱稱不得為空")
    @Size(max = 50, message = "暱稱不得超過 50 字")
    private String nickname;

    @NotBlank(message = "密碼不得為空")
    @Size(min = 6, max = 50, message = "密碼長度需在 6~50 字元之間")
    private String password;
}

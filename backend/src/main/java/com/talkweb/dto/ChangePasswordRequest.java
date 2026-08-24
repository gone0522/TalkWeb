package com.talkweb.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {
    private String oldPassword; // 首次改密若帶一次性密碼可驗證，或登入後由 JWT 識別

    @NotBlank(message = "新密碼不得為空")
    @Size(min = 6, max = 50, message = "新密碼長度需在 6~50 字元之間")
    private String newPassword;
}

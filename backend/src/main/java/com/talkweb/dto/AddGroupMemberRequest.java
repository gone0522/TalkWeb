package com.talkweb.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddGroupMemberRequest {
    @NotEmpty(message = "欲邀請的使用者 ID 清單不得為空")
    private List<Long> userIds;
}

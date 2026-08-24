package com.talkweb.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CreateGroupRequest {
    @NotBlank(message = "群組名稱不得為空")
    private String name;

    private Integer icon;

    private String announcement;

    @NotEmpty(message = "群組初始成員不得為空")
    private List<Long> memberIds;
}

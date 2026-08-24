package com.talkweb.controller;

import com.talkweb.dto.ApiResponse;
import com.talkweb.dto.LinkPreviewDto;
import com.talkweb.service.LinkPreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/utils")
@RequiredArgsConstructor
public class UtilController {

    private final LinkPreviewService linkPreviewService;

    @GetMapping("/link-preview")
    public ApiResponse<LinkPreviewDto> getLinkPreview(@RequestParam("url") String url) {
        LinkPreviewDto preview = linkPreviewService.fetchPreview(url);
        return ApiResponse.success(preview);
    }
}

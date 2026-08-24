package com.talkweb.service;

import com.talkweb.dto.LinkPreviewDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LinkPreviewServiceTest {

    private final LinkPreviewService linkPreviewService = new LinkPreviewService();

    @Test
    @DisplayName("測試從文字中擷取首個 URL 網址")
    void testExtractFirstUrl() {
        String text1 = "請參考這份規格文件：https://github.com/talkweb 謝謝！";
        assertEquals("https://github.com/talkweb", linkPreviewService.extractFirstUrl(text1));

        String text2 = "純文字沒有任何網址";
        assertNull(linkPreviewService.extractFirstUrl(text2));

        String text3 = "多個網址 http://test.com 與 https://google.com";
        assertEquals("http://test.com", linkPreviewService.extractFirstUrl(text3));
    }

    @Test
    @DisplayName("測試當網址無效或連線超時時能安全 fallback 回傳包含 domain 的 DTO")
    void testFetchPreviewFallback() {
        LinkPreviewDto preview = linkPreviewService.fetchPreview("https://invalid-non-existent-domain-talkweb.internal");
        assertNotNull(preview);
        assertEquals("invalid-non-existent-domain-talkweb.internal", preview.getDomain());
        assertNotNull(preview.getUrl());
    }
}

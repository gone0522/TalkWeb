package com.talkweb.service;

import com.talkweb.dto.LinkPreviewDto;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class LinkPreviewService {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "\\b(https?://[a-zA-Z0-9+&@#/%?=~_|!:,.;]*[a-zA-Z0-9+&@#/%=~_|])",
            Pattern.CASE_INSENSITIVE
    );

    private final Map<String, LinkPreviewDto> previewCache = new ConcurrentHashMap<>();

    public String extractFirstUrl(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = URL_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public LinkPreviewDto fetchPreview(String targetUrl) {
        if (targetUrl == null || targetUrl.isBlank()) {
            return null;
        }

        String normalizedUrl = targetUrl.trim();
        if (!normalizedUrl.startsWith("http://") && !normalizedUrl.startsWith("https://")) {
            normalizedUrl = "https://" + normalizedUrl;
        }

        if (previewCache.containsKey(normalizedUrl)) {
            return previewCache.get(normalizedUrl);
        }

        try {
            URI uri = URI.create(normalizedUrl);
            String domain = uri.getHost();

            Document doc = Jsoup.connect(normalizedUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 TalkWeb-Preview/1.0")
                    .timeout(3000)
                    .maxBodySize(1024 * 1024) // 1MB max
                    .followRedirects(true)
                    .get();

            String title = getMetaContent(doc, "property", "og:title");
            if (title == null || title.isBlank()) {
                title = doc.title();
            }

            String description = getMetaContent(doc, "property", "og:description");
            if (description == null || description.isBlank()) {
                description = getMetaContent(doc, "name", "description");
            }

            String imageUrl = getMetaContent(doc, "property", "og:image");
            if (imageUrl != null && !imageUrl.startsWith("http")) {
                imageUrl = uri.resolve(imageUrl).toString();
            }

            LinkPreviewDto preview = LinkPreviewDto.builder()
                    .url(normalizedUrl)
                    .title(title != null ? title.trim() : domain)
                    .description(description != null ? description.trim() : "")
                    .imageUrl(imageUrl)
                    .domain(domain)
                    .build();

            // Cache if valid
            if (previewCache.size() < 1000) {
                previewCache.put(normalizedUrl, preview);
            }
            return preview;
        } catch (Exception e) {
            log.debug("無法獲取 URL 預覽 ({}) : {}", targetUrl, e.getMessage());
            try {
                URI uri = URI.create(normalizedUrl);
                return LinkPreviewDto.builder()
                        .url(normalizedUrl)
                        .title(uri.getHost())
                        .description("")
                        .domain(uri.getHost())
                        .build();
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private String getMetaContent(Document doc, String attrKey, String attrValue) {
        Element meta = doc.selectFirst("meta[" + attrKey + "=" + attrValue + "]");
        return meta != null ? meta.attr("content") : null;
    }
}

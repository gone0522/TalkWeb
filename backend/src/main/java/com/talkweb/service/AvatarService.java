package com.talkweb.service;

import com.talkweb.entity.User;
import com.talkweb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarService {

    private final UserRepository userRepository;

    @Value("${app.avatar.max-size-kb:500}")
    private long maxAvatarSizeKb;

    @Transactional
    public void uploadAvatar(Long userId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("頭像圖檔不得為空");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
            throw new IllegalArgumentException("僅支援 JPG、PNG 或 WebP 格式的圖片");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到使用者"));

        // Resize and crop to 200x200 square PNG
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(file.getBytes()))
                .size(200, 200)
                .crop(Positions.CENTER)
                .outputFormat("png")
                .outputQuality(0.85f)
                .toOutputStream(outputStream);

        byte[] resizedBytes = outputStream.toByteArray();
        if (resizedBytes.length > maxAvatarSizeKb * 1024) {
            log.warn("縮圖後檔案仍大於限制大小: {} bytes", resizedBytes.length);
        }

        user.setAvatarData(resizedBytes);
        user.setAvatarMimeType("image/png");
        userRepository.save(user);
        log.info("使用者 ID {} 成功更新頭像 (大小: {} bytes)", userId, resizedBytes.length);
    }

    @Transactional(readOnly = true)
    public byte[] getAvatarData(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到使用者"));
        return user.getAvatarData();
    }

    @Transactional(readOnly = true)
    public String getAvatarMimeType(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到使用者"));
        return user.getAvatarMimeType() != null ? user.getAvatarMimeType() : "image/png";
    }
}

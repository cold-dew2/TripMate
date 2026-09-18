package com.example.backend.trma.controller;

import com.example.backend.trma.dto.dataList.UploadData;
import com.example.backend.trma.dto.response.UploadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@RestController
public class UploadController {
    private final Path uploadRoot = Path.of("uploads").toAbsolutePath().normalize();

    @PostMapping("/uploads")
    public UploadResponse upload(@RequestParam("file") MultipartFile file,
                                  Authentication authentication) throws IOException {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        if (file.isEmpty() || file.getSize() > 10 * 1024 * 1024
                || !Objects.requireNonNullElse(file.getContentType(), "").startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "10MB 이하의 이미지 파일만 업로드할 수 있습니다.");
        }

        Files.createDirectories(uploadRoot);
        String original = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "image"));
        String extension = original.lastIndexOf('.') >= 0 ? original.substring(original.lastIndexOf('.')) : ".jpg";
        String filename = UUID.randomUUID() + extension.toLowerCase(Locale.ROOT);
        file.transferTo(uploadRoot.resolve(filename));

        return new UploadResponse(
                true,
                200,
                "SUCCESS",
                "업로드 성공",
                "/uploads",
                "",
                new UploadData("/uploads/" + filename, filename)
        );
    }
}

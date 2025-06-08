package org.example.epub_demo1.controller;

import io.micrometer.common.util.StringUtils;
import jakarta.annotation.PostConstruct;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.epub.EpubReader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.module.Configuration;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@CrossOrigin
public class Controller {

    private final Path rootLocation = Paths.get("uploads");
    private final Path coverLocation = Paths.get("covers");

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            Files.createDirectories(coverLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> handleFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            // 保存文件
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path targetPath = this.rootLocation.resolve(filename);
            Files.copy(file.getInputStream(), targetPath);

            // 解析 EPUB 元数据
            Map<String, String> metadata = parseEpubMetadata(file.getInputStream());

            // 返回结果（包含文件名和元数据）
            Map<String, Object> response = new HashMap<>();
            response.put("fileName", filename);
            response.put("metadata", metadata);
            response.put("coverImagePath", metadata.get("coverImagePath")); // 新增封面路径返回

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "上传失败: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    private <EpubBook> Map<String, String> parseEpubMetadata(InputStream epubStream) {
        Map<String, String> metadata = new HashMap<>();
        try {
            EpubBook epubBook = (EpubBook) new EpubReader().readEpub(epubStream);
            Metadata meta = ((nl.siegmann.epublib.domain.Book) epubBook).getMetadata();

            // 解析标题
            String title = meta.getFirstTitle();
            if (StringUtils.isBlank(title)) {
                title = "未知标题";
            }
            metadata.put("title", title);

            // 解析作者
            String author = meta.getAuthors().stream()
                    .findFirst()
                    .map(Author::toString)
                    .orElse("未知作者");
            metadata.put("author", author);

            // 解析描述
            String description = meta.getDescriptions().stream()
                    .findFirst()
                    .orElse("");
            metadata.put("description", description);

            // 修复：正确的封面获取方式
            Resource coverImage = ((nl.siegmann.epublib.domain.Book) epubBook).getCoverImage();
            if (coverImage != null) {
                // 生成唯一的封面文件名
                String coverFileName = "cover_" + UUID.randomUUID() + getImageExtension(coverImage);
                Path coverPath = this.coverLocation.resolve(coverFileName);

                // 保存封面图片
                try (OutputStream out = Files.newOutputStream(coverPath)) {
                    out.write(coverImage.getData());
                }
                metadata.put("coverImagePath", coverFileName);
            } else {
                metadata.put("coverImagePath", "default-cover.jpg");
            }

        } catch (Exception e) {
            // 解析失败时设置默认值
            metadata.put("title", "未知标题");
            metadata.put("author", "未知作者");
            metadata.put("description", "");
            metadata.put("cover_image_path", "default-cover.jpg");
        }
        return metadata;
    }

    // 获取图片扩展名
    private String getImageExtension(Resource resource) {
        try {
            String mimeType = resource.getMediaType().toString();
            if (mimeType.equals("image/jpeg")) return ".jpg";
            if (mimeType.equals("image/png")) return ".png";
            return ".jpg"; // 默认
        } catch (Exception e) {
            return ".jpg";
        }
    }
}

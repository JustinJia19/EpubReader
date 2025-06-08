package org.example.epub_demo1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${cover.upload-dir}") // 新增封面目录配置
    private String coverDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // EPUB文件资源映射（保持原有）
        String epubResourceLocation = "file:" + Paths.get(uploadDir).normalize().toAbsolutePath() + "/";
        System.out.println("[CONFIG] EPUB资源位置：" + epubResourceLocation);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(epubResourceLocation)
                .setCachePeriod(0)
                .resourceChain(true)
                .addResolver(new AdvancedPathResourceResolver());

        // 新增封面图片资源映射
        String coverResourceLocation = "file:" + Paths.get(coverDir).normalize().toAbsolutePath() + "/";
        System.out.println("[CONFIG] 封面资源位置：" + coverResourceLocation);

        registry.addResourceHandler("/covers/**")
                .addResourceLocations(coverResourceLocation)
                .setCachePeriod(86400) // 缓存24小时（封面不常更新）
                .resourceChain(true)
                .addResolver(new AdvancedPathResourceResolver());
    }

    // 自定义资源解析器（保持不变）
    private static class AdvancedPathResourceResolver extends PathResourceResolver {
        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            // 双重解码处理特殊字符
            String decodedPath = URLDecoder.decode(
                    URLDecoder.decode(resourcePath, StandardCharsets.UTF_8),
                    StandardCharsets.UTF_8
            );

            System.out.println("[REQUEST] 请求路径：" + decodedPath);

            Resource requestedResource = location.createRelative(decodedPath);
            if (requestedResource.exists() && requestedResource.isReadable()) {
                return requestedResource;
            }

            System.err.println("[ERROR] 文件访问失败：" + requestedResource.getURI());
            throw new NoSuchFileException("Requested file not found");
        }
    }
}
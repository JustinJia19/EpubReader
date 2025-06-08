package org.example.epub_demo1.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class UploadConfig implements WebMvcConfigurer {
        // 配置文件上传的临时存储位置
        @Bean
        public MultipartConfigElement multipartConfigElement() {
            MultipartConfigFactory factory = new MultipartConfigFactory();
            factory.setLocation(System.getProperty("java.io.tmpdir")); // 使用系统临时目录
            return factory.createMultipartConfig();
        }
}

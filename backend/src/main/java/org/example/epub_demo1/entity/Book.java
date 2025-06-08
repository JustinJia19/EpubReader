package org.example.epub_demo1.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.epub_demo1.entity.enums.BookCategory;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Book implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String author;
    private String description;
    private BookCategory category; // 对应ENUM的Java枚举
    @JsonProperty("coverImagePath")
    private String coverImagePath; // 可空
    private String epubFileName; // 可空
    private Long uploadUserId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;

    // 添加封面URL字段（非数据库字段）
    @JsonIgnore
    private String coverUrl;

    public String getCoverUrl() {
        if (coverImagePath == null || coverImagePath.isEmpty()) {
            return "/covers/default-cover.jpg";
        }
        return "/covers/" + coverImagePath;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCoverImagePath() {
        return coverImagePath;
    }

    public void setCoverImagePath(String coverImagePath) {
        this.coverImagePath = coverImagePath;
    }

    public BookCategory getCategory() {
        return category;
    }

    public void setCategory(BookCategory category) {
        this.category = category;
    }

    public String getEpubFileName() {
        return epubFileName;
    }

    public void setEpubFileName(String epubFileName) {
        this.epubFileName = epubFileName;
    }

    public Long getUploadUserId() {
        return uploadUserId;
    }

    public void setUploadUserId(Long uploadUserId) {
        this.uploadUserId = uploadUserId;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }
}

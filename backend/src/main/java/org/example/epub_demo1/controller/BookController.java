package org.example.epub_demo1.controller;

import org.example.epub_demo1.entity.Book;
import org.example.epub_demo1.entity.enums.BookCategory;
import org.example.epub_demo1.mapper.BookMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

@RestController
@CrossOrigin
public class BookController {

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BookCacheService bookCacheService;

    // 查询书籍 - 公开访问
    @GetMapping("/getall")
    public ResponseEntity<List<Book>> getAll(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String category
    ) {
        try {
            // 清理参数（空字符串转为null）
            title = cleanParam(title);
            author = cleanParam(author);
            category = cleanParam(category);

            // 有查询条件时不使用缓存
            if (hasSearchCriteria(title, author, category)) {
                List<Book> books = bookMapper.getAll(title, author, category);
                return ResponseEntity.ok(books);
            }

            // 尝试从缓存获取所有书籍
            List<Book> cachedBooks = bookCacheService.getAllBooksFromCache();
            if (cachedBooks != null) {
                return ResponseEntity.ok(cachedBooks);
            }

            // 缓存未命中则查询数据库并缓存结果
            List<Book> books = bookMapper.getAll(null, null, null);
            bookCacheService.cacheAllBooks(books);
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            logError("获取书籍列表失败", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // 删除书籍 - 需要认证
    @DeleteMapping("/delebook")
    public ResponseEntity<String> deleteBook(@RequestParam("id") int id) {
        try {
            // 先查询书籍是否存在
            Book book = bookMapper.selectById((long) id);
            if (book == null) {
                return ResponseEntity.badRequest().body("书籍不存在");
            }

            int result = bookMapper.DeleBook(id);
            if (result > 0) {
                // 清除缓存
                bookCacheService.clearBookCache((long) id);
                bookCacheService.clearAllBooksCache();
                return ResponseEntity.ok("书籍删除成功");
            }
            return ResponseEntity.badRequest().body("书籍删除失败");
        } catch (Exception e) {
            logError("删除书籍失败 ID: " + id, e);
            return ResponseEntity.internalServerError().body("服务器错误: " + e.getMessage());
        }
    }

    // 添加书籍 - 需要认证
    @PostMapping("/addbook")
    public ResponseEntity<String> addBook(@RequestBody Book book) {
        try {
            book.setUploadTime(LocalDateTime.now());

            // 确保封面路径不为空
            if (book.getCoverImagePath() == null || book.getCoverImagePath().isEmpty()) {
                // 尝试从封面URL中提取路径
                if (book.getCoverUrl() != null) {
                    String coverPath = book.getCoverUrl().substring(book.getCoverUrl().lastIndexOf("/") + 1);
                    book.setCoverImagePath(coverPath);
                    System.out.println("从coverUrl提取封面路径: " + coverPath);
                } else {
                    book.setCoverImagePath("default-cover.jpg");
                    System.out.println("使用默认封面路径");
                }
            }

            System.out.println("添加书籍 - 封面路径: " + book.getCoverImagePath());

            int result = bookMapper.addBook(book);
            if (result > 0) {
                // 清除缓存
                bookCacheService.clearAllBooksCache();
                return ResponseEntity.ok("书籍添加成功");
            }
            return ResponseEntity.badRequest().body("书籍添加失败");
        } catch (Exception e) {
            logError("添加书籍失败", e);
            return ResponseEntity.internalServerError().body("服务器错误: " + e.getMessage());
        }
    }

    // 修改书籍 - 需要认证
    @PutMapping("/upbook")
    public ResponseEntity<String> updateBook(@RequestBody Book book) {
        try {
            // 先查询书籍是否存在
            Book existingBook = bookMapper.selectById(book.getId());
            if (existingBook == null) {
                return ResponseEntity.badRequest().body("书籍不存在");
            }

            int result = bookMapper.upBook(book);
            if (result > 0) {
                // 清除缓存
                bookCacheService.clearBookCache(book.getId());
                bookCacheService.clearAllBooksCache();
                return ResponseEntity.ok("书籍更新成功");
            }
            return ResponseEntity.badRequest().body("书籍更新失败");
        } catch (Exception e) {
            logError("更新书籍失败 ID: " + book.getId(), e);
            return ResponseEntity.internalServerError().body("服务器错误: " + e.getMessage());
        }
    }

    // 获取统计信息
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getBookStatistics() {
        try {
            // 尝试从缓存获取
            Map<String, Object> cachedStats = bookCacheService.getStatsFromCache();
            if (cachedStats != null) {
                return ResponseEntity.ok(cachedStats);
            }

            // 缓存未命中则查询数据库
            int totalBooks = bookMapper.countTotalBooks();
            List<Map<String, Object>> categoryStats = bookMapper.countBooksByCategory();

            // 将枚举值转换为中文
            Map<String, String> categoryMap = new HashMap<>();
            for (BookCategory category : BookCategory.values()) {
                categoryMap.put(category.name(), category.getChineseName());
            }

            // 处理统计结果
            List<Map<String, Object>> formattedStats = new ArrayList<>();
            for (Map<String, Object> stat : categoryStats) {
                String category = (String) stat.get("category");
                int count = ((Long) stat.get("count")).intValue();

                formattedStats.add(Map.of(
                        "name", categoryMap.getOrDefault(category, "其他分类"),
                        "count", count
                ));
            }

            Map<String, Object> stats = Map.of(
                    "total", totalBooks,
                    "categories", formattedStats
            );

            // 缓存统计结果
            bookCacheService.cacheStats(stats);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logError("获取统计信息失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "获取统计数据失败: " + e.getMessage()
            ));
        }
    }

    // ============= 辅助方法 =============

    private boolean hasSearchCriteria(String title, String author, String category) {
        return StringUtils.hasText(title) ||
                StringUtils.hasText(author) ||
                StringUtils.hasText(category);
    }

    private String cleanParam(String param) {
        return StringUtils.hasText(param) ? param.trim() : null;
    }

    private void logError(String message, Exception e) {
        System.err.println("ERROR: " + message);
        e.printStackTrace();
    }
}
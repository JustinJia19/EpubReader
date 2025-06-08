package org.example.epub_demo1.controller;

import org.example.epub_demo1.entity.Book;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class BookCacheService {
    private static final String ALL_BOOKS_KEY = "books:all";
    private static final String BOOK_KEY_PREFIX = "book:";
    private static final String STATS_KEY = "books:stats";
    private static final Duration BOOKS_TTL = Duration.ofMinutes(30);
    private static final Duration STATS_TTL = Duration.ofHours(1);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 缓存所有书籍列表
    public void cacheAllBooks(List<Book> books) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(ALL_BOOKS_KEY, books, BOOKS_TTL);
    }

    // 获取缓存的书籍列表
    public List<Book> getAllBooksFromCache() {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return (List<Book>) ops.get(ALL_BOOKS_KEY);
    }

    // 缓存单本书籍
    public void cacheBook(Book book) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(BOOK_KEY_PREFIX + book.getId(), book, BOOKS_TTL);
    }

    // 获取缓存的单本书籍
    public Book getBookFromCache(Long id) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return (Book) ops.get(BOOK_KEY_PREFIX + id);
    }

    // 清除所有书籍缓存
    public void clearAllBooksCache() {
        redisTemplate.delete(ALL_BOOKS_KEY);
    }

    // 清除单本书籍缓存
    public void clearBookCache(Long id) {
        redisTemplate.delete(BOOK_KEY_PREFIX + id);
    }

    // 防止缓存穿透的空对象缓存
    public void cacheEmptyBook(Long id) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(BOOK_KEY_PREFIX + id, new Book(), Duration.ofMinutes(5));
    }

    // 缓存统计信息
    public void cacheStats(Map<String, Object> stats) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ops.set(STATS_KEY, stats, STATS_TTL);
    }

    // 获取缓存的统计信息
    public Map<String, Object> getStatsFromCache() {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        return (Map<String, Object>) ops.get(STATS_KEY);
    }

    // 清除统计缓存
    public void clearStatsCache() {
        redisTemplate.delete(STATS_KEY);
    }
}
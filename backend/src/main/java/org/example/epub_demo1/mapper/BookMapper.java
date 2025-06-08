package org.example.epub_demo1.mapper;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import org.example.epub_demo1.entity.Book;

import java.util.List;
import java.util.Map;

@Mapper
public interface BookMapper {
    //查询所有书籍信息
    // 动态查询（使用SQL构建器）
    @SelectProvider(type = BookSqlBuilder.class, method = "buildGetAllSql")
    List<Book> getAll(
            @Param("title") String title,
            @Param("author") String author,
            @Param("category") String category
    );

    // 添加根据ID查询单本书籍的方法
    @Select("SELECT * FROM books WHERE id = #{id}")
    Book selectById(Long id);

    //删除书籍
    @Delete("delete from books where id = #{id}")
    int DeleBook(long id);

    // 添加书籍信息
    @Insert({
            "<script>",
            "INSERT INTO books (",
            "title, author, description, category, cover_image_path, epub_file_name, upload_time",
            ") VALUES (",
            "#{title}, #{author}, #{description}, #{category}, #{coverImagePath}, #{epubFileName}, #{uploadTime}",
            ")",
            "</script>"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id") // 确保自增 ID 回填
    int addBook(Book book);

    // 修改书籍信息
    @Update({
            "<script>",
            "UPDATE books",
            "SET",
            "title = #{title},",
            "author = #{author},",
            "description = #{description},",
            "category = #{category},",
            "cover_image_path = #{coverImagePath},",
            "epub_file_name = #{epubFileName},",
            "upload_time = #{uploadTime}",
            "WHERE id = #{id}",
            "</script>"
    })
    int upBook(Book book);

    // SQL构建器类
    class BookSqlBuilder {
        public String buildGetAllSql(Map<String, Object> params) {
            return new SQL() {{
                SELECT("*");
                FROM("books");
                if (hasValue(params.get("title"))) {
                    WHERE("title LIKE CONCAT('%', #{title}, '%')");
                }
                if (hasValue(params.get("author"))) {
                    WHERE("author LIKE CONCAT('%', #{author}, '%')");
                }
                if (hasValue(params.get("category"))) {
                    WHERE("category = #{category}");
                }
            }}.toString();
        }

        private boolean hasValue(Object value) {
            return value != null && !value.toString().isEmpty();
        }
    }

    @Select("SELECT COUNT(*) FROM books")
    int countTotalBooks();

    @Select("SELECT category, COUNT(*) as count FROM books GROUP BY category")
    List<Map<String, Object>> countBooksByCategory();
}

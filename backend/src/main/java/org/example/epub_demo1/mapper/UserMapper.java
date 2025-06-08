package org.example.epub_demo1.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.epub_demo1.entity.User;

@Mapper
public interface UserMapper {
    @Select("select * from user where username = #{username} and password = #{password}")
    User login(User user);

    @Select("SELECT id, username, password FROM users WHERE username = #{username}")
    User findByUsername(String username);
}

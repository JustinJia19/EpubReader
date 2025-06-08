package org.example.epub_demo1.controller;

import org.example.epub_demo1.entity.User;
import org.example.epub_demo1.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@RestController
@CrossOrigin
public class UserController {
    @Autowired
    private DataSource dataSource;

    /*@PostMapping("/login")
    public Object login(@RequestBody LoginRequest request){
        try(Connection conn = dataSource.getConnection()){
            PreparedStatement stmt = conn.prepareStatement(
                    "SELECT * FROM users WHERE username=? AND password=?"
            );
            stmt.setString(1,request.getUsername());
            stmt.setString(2,request.getPassword());

            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                return user;
            }
            else{
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "System error:"+e.getMessage();
        }
    }*/

    static class LoginRequest{
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}

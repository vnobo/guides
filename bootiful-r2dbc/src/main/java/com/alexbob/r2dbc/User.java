package com.alexbob.r2dbc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * com.alexbob.r2dbc.User
 *
 * @author Alex bob(https://github.com/vnobo)
 * @date Created by 2019/12/15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("users")
public class User {

    @Id
    private Integer id;
    private String username,password;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}

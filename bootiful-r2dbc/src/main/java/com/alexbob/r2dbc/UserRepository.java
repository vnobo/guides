package com.alexbob.r2dbc;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * com.alexbob.r2dbc.UserRepository
 *
 * @author Alex bob(https://github.com/vnobo)
 * @date Created by 2019/12/15
 */
public interface UserRepository extends ReactiveCrudRepository<User, Integer> {

    @Query("SELECT * FROM users WHERE username = :username")
    Flux<User> findByUsername(String username);
}

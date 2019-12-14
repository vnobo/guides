package com.alexbob.r2dbc;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.DatabaseClient;

import java.time.Duration;
import java.util.Arrays;

/**
 * ${PACKAGE_NAME}.${NAME}
 *
 * @author Alex bob(https://github.com/vnobo)
 * @date Created by ${DATE}
 */
@Log4j2
@SpringBootApplication
public class R2dbcApplication {

    public static void main(String[] args) {
        SpringApplication.run(R2dbcApplication.class, args);
    }

    @Bean
    public CommandLineRunner demo(UserRepository repository,DatabaseClient databaseClient) {
        return (args) -> {
            // save a few customers
            repository.saveAll(Arrays.asList(new User("Jack", "Bauer"),
                    new User("Chloe", "O'Brian"),
                    new User("Kim", "Bauer"),
                    new User("David", "Palmer"),
                    new User("Michelle", "Dessler")))
                    .blockLast(Duration.ofSeconds(10));

            // fetch all customers
            log.info("Customers found with findAll():");
            log.info("-------------------------------");
            repository.findAll().doOnNext(customer -> {
                log.info(customer.toString());
            }).blockLast(Duration.ofSeconds(10));

            log.info("");

            // fetch an individual customer by ID
            repository.findById(1).doOnNext(customer -> {
                log.info("Customer found with findById(1L):");
                log.info("--------------------------------");
                log.info(customer.toString());
                log.info("");
            }).block(Duration.ofSeconds(10));


            // fetch customers by last name
            log.info("Customer found with findByLastName('Bauer'):");
            log.info("--------------------------------------------");
            repository.findByUsername("Bauer").doOnNext(bauer -> {
                log.info(bauer.toString());
            }).blockLast(Duration.ofSeconds(10));
            ;
            log.info("");
        };
    }
}

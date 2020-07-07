package com.alexbob.web;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;

@Log4j2
@SpringBootApplication
public class WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }

    @Autowired
    SyncService syncService;

    @Bean
    public CommandLineRunner start() {
        return args -> {
            Flux.range(0, 1000)
                    .publishOn(Schedulers.parallel())
                    .delayElements(Duration.ofMillis(200))
                    .subscribe(x -> {
                        this.syncService.setX(x);
                        this.syncService.log();
                    });
            log.info("=========================");
           this.syncService.log();
        };
    }
}

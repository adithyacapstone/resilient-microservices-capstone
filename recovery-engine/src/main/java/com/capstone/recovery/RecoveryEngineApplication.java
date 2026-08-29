package com.capstone.recovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RecoveryEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecoveryEngineApplication.class, args);
    }
}
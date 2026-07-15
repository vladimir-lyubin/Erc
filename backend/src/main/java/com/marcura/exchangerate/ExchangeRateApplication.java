package com.marcura.exchangerate;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT10M")
public class ExchangeRateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExchangeRateApplication.class, args);
    }
}

package com.scraper.triescraper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TrieScraperApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrieScraperApplication.class, args);
    }
}
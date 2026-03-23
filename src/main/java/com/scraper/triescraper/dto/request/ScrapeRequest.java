package com.scraper.triescraper.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ScrapeRequest {

    @NotEmpty(message = "URLs cannot be empty")
    private List<String> urls;

    @NotEmpty(message = "Keywords cannot be empty")
    private List<String> keywords;

    private LocalDateTime schedule;
}
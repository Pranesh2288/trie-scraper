package com.scraper.triescraper.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchRequest {

    @NotBlank(message = "Prefix cannot be blank")
    private String prefix;

    private int limit = 5;
}
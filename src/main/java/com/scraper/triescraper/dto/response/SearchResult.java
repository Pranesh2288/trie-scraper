package com.scraper.triescraper.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private String url;
    private String matchedContent;
    private LocalDateTime timestamp;
}
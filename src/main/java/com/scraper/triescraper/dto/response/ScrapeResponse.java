package com.scraper.triescraper.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapeResponse {

    private String status;
    private String message;
    private String jobId;
    private LocalDateTime scheduledAt;
}
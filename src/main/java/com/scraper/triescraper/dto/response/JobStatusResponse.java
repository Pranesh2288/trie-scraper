package com.scraper.triescraper.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatusResponse {

    private String status;
    private String jobId;
    private List<String> urlsScraped;
    private List<String> keywordsFound;
    private String dataSize;
    private LocalDateTime finishedAt;
}
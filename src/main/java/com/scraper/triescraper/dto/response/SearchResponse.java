package com.scraper.triescraper.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {

    private String status;
    private List<SearchResult> results;
}
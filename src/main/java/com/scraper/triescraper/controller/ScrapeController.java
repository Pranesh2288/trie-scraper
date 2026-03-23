package com.scraper.triescraper.controller;

import com.scraper.triescraper.dto.request.ScrapeRequest;
import com.scraper.triescraper.dto.request.SearchRequest;
import com.scraper.triescraper.dto.response.JobStatusResponse;
import com.scraper.triescraper.dto.response.ScrapeResponse;
import com.scraper.triescraper.dto.response.SearchResponse;
import com.scraper.triescraper.exception.ResourceNotFoundException;
import com.scraper.triescraper.service.ScrapeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ScrapeController {

    private final ScrapeService scrapeService;

    // POST /api/v1/scrape
    @PostMapping("/scrape")
    public ResponseEntity<ScrapeResponse> initiateScrape(
            @Valid @RequestBody ScrapeRequest request) {

        ScrapeResponse response = scrapeService.initiateScrape(request);
        return ResponseEntity.ok(response);
    }

    // POST /api/v1/search
    @PostMapping("/search")
    public ResponseEntity<SearchResponse> search(
            @Valid @RequestBody SearchRequest request) {

        SearchResponse response = scrapeService.search(request);
        return ResponseEntity.ok(response);
    }

    // GET /api/v1/status/{jobId}
    @GetMapping("/status/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(
            @PathVariable String jobId) {

        JobStatusResponse response = scrapeService.getJobStatus(jobId);

        if ("NOT_FOUND".equals(response.getStatus())) {
            throw new ResourceNotFoundException(
                    "Job not found with ID: " + jobId);
        }

        return ResponseEntity.ok(response);
    }
}
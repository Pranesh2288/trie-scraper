package com.scraper.triescraper.service;

import com.scraper.triescraper.dto.request.ScrapeRequest;
import com.scraper.triescraper.dto.request.SearchRequest;
import com.scraper.triescraper.dto.response.JobStatusResponse;
import com.scraper.triescraper.dto.response.ScrapeResponse;
import com.scraper.triescraper.dto.response.SearchResponse;

public interface ScrapeService {

    ScrapeResponse initiateScrape(ScrapeRequest request);

    SearchResponse search(SearchRequest request);

    JobStatusResponse getJobStatus(String jobId);
}
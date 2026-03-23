package com.scraper.triescraper.service.impl;

import com.scraper.triescraper.dto.request.ScrapeRequest;
import com.scraper.triescraper.dto.request.SearchRequest;
import com.scraper.triescraper.dto.response.*;
import com.scraper.triescraper.model.entity.ScrapedData;
import com.scraper.triescraper.model.entity.ScrapeJob;
import com.scraper.triescraper.repository.ScrapedDataRepository;
import com.scraper.triescraper.repository.ScrapeJobRepository;
import com.scraper.triescraper.service.ScrapeService;
import com.scraper.triescraper.trie.Trie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapeServiceImpl implements ScrapeService {

    private final ScrapedDataRepository scrapedDataRepository;
    private final ScrapeJobRepository scrapeJobRepository;
    private final Trie trie;

    // ← SEPARATE BEAN: handles all DB writes with real transactions
    private final ScrapeJobPersistenceService persistenceService;

    @Override
    @Transactional
    public ScrapeResponse initiateScrape(ScrapeRequest request) {
        String jobId = UUID.randomUUID().toString();

        ScrapeJob job = ScrapeJob.builder()
                .jobId(jobId)
                .urls(new ArrayList<>(request.getUrls()))
                .keywords(new ArrayList<>(request.getKeywords()))
                .status("PENDING")
                .scheduledAt(request.getSchedule() != null
                        ? request.getSchedule()
                        : LocalDateTime.now())
                .build();

        scrapeJobRepository.save(job);

        if (request.getSchedule() == null ||
            !request.getSchedule().isAfter(LocalDateTime.now())) {
            performScrape(jobId);
        }

        return ScrapeResponse.builder()
                .status("success")
                .message("Scraping initiated successfully.")
                .jobId(jobId)
                .scheduledAt(job.getScheduledAt())
                .build();
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        String prefix = request.getPrefix().toLowerCase();
        int limit = request.getLimit() > 0 ? request.getLimit() : 5;

        List<String> matchedIds = trie.getScrapedDataIds(prefix, limit);
        List<SearchResult> results = new ArrayList<>();

        if (!matchedIds.isEmpty()) {
            List<ScrapedData> dataList =
                    scrapedDataRepository.findAllById(matchedIds);
            for (ScrapedData data : dataList) {
                results.add(SearchResult.builder()
                        .url(data.getSourceUrl())
                        .matchedContent(data.getMatchedContent())
                        .timestamp(data.getScrapedAt())
                        .build());
            }
        }

        if (results.isEmpty()) {
            log.warn("Trie returned no IDs for prefix '{}'. " +
                    "Falling back to DB.", prefix);
            List<ScrapedData> fallback =
                    scrapedDataRepository
                            .findByKeywordStartingWithIgnoreCase(prefix);
            fallback.stream().limit(limit).forEach(data ->
                    results.add(SearchResult.builder()
                            .url(data.getSourceUrl())
                            .matchedContent(data.getMatchedContent())
                            .timestamp(data.getScrapedAt())
                            .build()));
        }

        return SearchResponse.builder()
                .status("success")
                .results(results)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public JobStatusResponse getJobStatus(String jobId) {
        ScrapeJob job = scrapeJobRepository.findByJobId(jobId);

        if (job == null) {
            return JobStatusResponse.builder()
                    .status("NOT_FOUND")
                    .jobId(jobId)
                    .build();
        }

        return JobStatusResponse.builder()
                .status(job.getStatus())
                .jobId(job.getJobId())
                .urlsScraped(job.getUrls())
                .keywordsFound(job.getKeywords())
                .dataSize(job.getDataSize())
                .finishedAt(job.getFinishedAt())
                .build();
    }

    // ─── Core scraping logic ─────────────────────────────────────────
    // NO @Transactional here — Jsoup HTTP calls must be
    // completely outside any DB transaction
    // All DB operations delegated to persistenceService
    // which is a SEPARATE BEAN — Spring proxy works correctly
    public void performScrape(String jobId) {
        log.info("performScrape() started for jobId: {}", jobId);

        // Step 1: Mark IN_PROGRESS via separate bean
        // @Transactional fires correctly on external bean call
        ScrapeJob job = persistenceService.updateJobStatus(
                jobId, "IN_PROGRESS");

        if (job == null) {
            log.error("Aborting scrape — job not found: {}", jobId);
            return;
        }

        // Step 2: Copy collections to local variables immediately
        // Safe from any lazy loading issues after this point
        List<String> urls = new ArrayList<>(job.getUrls());
        List<String> keywords = new ArrayList<>(job.getKeywords());

        log.info("Scraping job: {} | URLs: {} | Keywords: {}",
                jobId, urls, keywords);

        // Step 3: All Jsoup HTTP calls — NO transaction open
        long totalBytes = 0;
        List<ScrapedDataHolder> scrapedItems = new ArrayList<>();

        for (String url : urls) {
            for (String keyword : keywords) {
                try {
                    Document doc = Jsoup.connect(url)
                            .userAgent("Mozilla/5.0 (Windows NT 10.0; " +
                                       "Win64; x64) AppleWebKit/537.36 " +
                                       "(KHTML, like Gecko) " +
                                       "Chrome/120.0.0.0 Safari/537.36")
                            .header("Accept-Language", "en-US,en;q=0.9")
                            .header("Accept",
                                    "text/html,application/xhtml+xml")
                            .timeout(15000)
                            .followRedirects(true)
                            .ignoreHttpErrors(true)
                            .get();

                    String bodyText = doc.body().text();
                    String lowerBody = bodyText.toLowerCase();
                    String lowerKeyword = keyword.toLowerCase();

                    if (lowerBody.contains(lowerKeyword)) {
                        int index = lowerBody.indexOf(lowerKeyword);
                        int start = Math.max(0, index - 100);
                        int end = Math.min(bodyText.length(),
                                index + 200);
                        String snippet = bodyText.substring(start, end);

                        scrapedItems.add(new ScrapedDataHolder(
                                url, lowerKeyword, snippet));
                        totalBytes += snippet.getBytes().length;

                        log.info("Match found: keyword='{}' url='{}'",
                                lowerKeyword, url);
                    } else {
                        log.info("No match: keyword='{}' url='{}'",
                                lowerKeyword, url);
                    }

                } catch (Exception e) {
                    log.error("Failed to scrape URL: {} | Error: {}",
                            url, e.getMessage());
                }
            }
        }

        // Step 4: Save all results and index Trie via separate bean
        // @Transactional fires correctly here
        persistenceService.saveScrapedDataAndIndexTrie(jobId, scrapedItems);

        // Step 5: Mark COMPLETED via separate bean
        // @Transactional fires correctly here
        persistenceService.updateJobStatusCompleted(jobId, totalBytes);

        log.info("performScrape() finished for jobId: {} | " +
                "items: {}", jobId, scrapedItems.size());
    }

    // Simple record to hold scraped data between Jsoup and DB steps
    public record ScrapedDataHolder(
            String url, String keyword, String snippet) {}
}
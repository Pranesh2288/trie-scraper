package com.scraper.triescraper.service.impl;

import com.scraper.triescraper.model.entity.ScrapedData;
import com.scraper.triescraper.model.entity.ScrapeJob;
import com.scraper.triescraper.repository.ScrapedDataRepository;
import com.scraper.triescraper.repository.ScrapeJobRepository;
import com.scraper.triescraper.trie.Trie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScrapeJobPersistenceService {

    private final ScrapeJobRepository scrapeJobRepository;
    private final ScrapedDataRepository scrapedDataRepository;
    private final Trie trie;

    // Loads job from DB and updates its status
    // Called from performScrape() via SEPARATE bean
    // so Spring proxy intercepts @Transactional correctly
    @Transactional
    public ScrapeJob updateJobStatus(String jobId, String status) {
        ScrapeJob job = scrapeJobRepository.findByJobId(jobId);
        if (job == null) {
            log.error("Job not found for ID: {}", jobId);
            return null;
        }
        job.setStatus(status);
        ScrapeJob saved = scrapeJobRepository.save(job);
        log.info("Job {} status updated to {}", jobId, status);
        return saved;
    }

    // Saves scraped data to DB and inserts into Trie
    // Called from performScrape() via SEPARATE bean
    @Transactional
    public void saveScrapedDataAndIndexTrie(String jobId,
            List<ScrapeServiceImpl.ScrapedDataHolder> items) {
        for (ScrapeServiceImpl.ScrapedDataHolder item : items) {
            ScrapedData data = ScrapedData.builder()
                    .jobId(jobId)
                    .sourceUrl(item.url())
                    .keyword(item.keyword())
                    .matchedContent(item.snippet())
                    .scrapedAt(LocalDateTime.now())
                    .build();

            ScrapedData saved = scrapedDataRepository.save(data);
            trie.insert(item.keyword(), saved.getId());
            log.info("Indexed: keyword='{}' id='{}'",
                    item.keyword(), saved.getId());
        }
    }

    // Marks job COMPLETED with final metadata
    // Called from performScrape() via SEPARATE bean
    @Transactional
    public void updateJobStatusCompleted(String jobId, long totalBytes) {
        ScrapeJob job = scrapeJobRepository.findByJobId(jobId);
        if (job == null) {
            log.error("Job not found for completion: {}", jobId);
            return;
        }
        job.setStatus("COMPLETED");
        job.setFinishedAt(LocalDateTime.now());
        job.setDataSize(formatSize(totalBytes));
        scrapeJobRepository.save(job);
        log.info("Job {} marked COMPLETED, size={}", jobId,
                formatSize(totalBytes));
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return (bytes / (1024 * 1024)) + " MB";
    }
}
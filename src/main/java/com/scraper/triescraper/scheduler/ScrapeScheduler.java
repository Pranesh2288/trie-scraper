package com.scraper.triescraper.scheduler;

import com.scraper.triescraper.model.entity.ScrapeJob;
import com.scraper.triescraper.repository.ScrapeJobRepository;
import com.scraper.triescraper.service.impl.ScrapeServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScrapeScheduler {

    private final ScrapeJobRepository scrapeJobRepository;
    private final ScrapeServiceImpl scrapeServiceImpl;

    @Scheduled(fixedRate = 60000)
    public void processPendingJobs() {
        log.info("Scheduler running at: {}", LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();

        // Query 1: load pending jobs with URLs
        List<ScrapeJob> jobsWithUrls =
                scrapeJobRepository.findPendingJobsDueWithUrls(now);

        if (jobsWithUrls.isEmpty()) {
            log.info("No pending jobs found.");
            return;
        }

        // Query 2: load same pending jobs with keywords
        List<ScrapeJob> jobsWithKeywords =
                scrapeJobRepository.findPendingJobsDueWithKeywords(now);

        // Merge keywords into the jobs that have URLs
        // Use jobId as key to match both lists
        Map<String, List<String>> keywordsByJobId = jobsWithKeywords
                .stream()
                .collect(Collectors.toMap(
                        ScrapeJob::getJobId,
                        ScrapeJob::getKeywords
                ));

        jobsWithUrls.forEach(job ->
                job.setKeywords(keywordsByJobId.getOrDefault(
                        job.getJobId(), List.of()))
        );

        // Now each job has both urls AND keywords populated
        for (ScrapeJob job : jobsWithUrls) {
            log.info("Processing scheduled job: {} | URLs: {} | " +
                            "Keywords: {}",
                    job.getJobId(),
                    job.getUrls().size(),
                    job.getKeywords().size());
            try {
                scrapeServiceImpl.performScrape(job.getJobId());
            } catch (Exception e) {
                log.error("Scheduled job failed: {} | Error: {}",
                        job.getJobId(), e.getMessage());
            }
        }
    }
}
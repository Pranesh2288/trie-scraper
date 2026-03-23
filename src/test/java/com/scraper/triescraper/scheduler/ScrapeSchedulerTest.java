package com.scraper.triescraper.scheduler;

import com.scraper.triescraper.model.entity.ScrapeJob;
import com.scraper.triescraper.repository.ScrapeJobRepository;
import com.scraper.triescraper.service.impl.ScrapeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrapeSchedulerTest {

    @Mock
    private ScrapeJobRepository scrapeJobRepository;

    @Mock
    private ScrapeServiceImpl scrapeServiceImpl;

    @InjectMocks
    private ScrapeScheduler scrapeScheduler;

    private ScrapeJob jobWithUrls;
    private ScrapeJob jobWithKeywords;

    @BeforeEach
    void setUp() {
        jobWithUrls = ScrapeJob.builder()
                .jobId("job-1")
                .urls(new ArrayList<>(
                        List.of("https://example.com")))
                .keywords(new ArrayList<>()) // keywords loaded separately
                .status("PENDING")
                .scheduledAt(LocalDateTime.now().minusMinutes(5))
                .build();

        jobWithKeywords = ScrapeJob.builder()
                .jobId("job-1")
                .urls(new ArrayList<>()) // urls loaded separately
                .keywords(new ArrayList<>(List.of("technology")))
                .status("PENDING")
                .scheduledAt(LocalDateTime.now().minusMinutes(5))
                .build();
    }

    @Test
    void shouldUseTwoSeparateQueriesNotFindAll() {
        when(scrapeJobRepository.findPendingJobsDueWithUrls(
                any(LocalDateTime.class)))
                .thenReturn(List.of(jobWithUrls));
        when(scrapeJobRepository.findPendingJobsDueWithKeywords(
                any(LocalDateTime.class)))
                .thenReturn(List.of(jobWithKeywords));

        scrapeScheduler.processPendingJobs();

        // Must use two separate queries
        verify(scrapeJobRepository).findPendingJobsDueWithUrls(
                any(LocalDateTime.class));
        verify(scrapeJobRepository).findPendingJobsDueWithKeywords(
                any(LocalDateTime.class));

        // Must NOT use findAll
        verify(scrapeJobRepository, never()).findAll();
    }

    @Test
    void shouldMergeUrlsAndKeywordsBeforeCallingPerformScrape() {
        when(scrapeJobRepository.findPendingJobsDueWithUrls(
                any(LocalDateTime.class)))
                .thenReturn(List.of(jobWithUrls));
        when(scrapeJobRepository.findPendingJobsDueWithKeywords(
                any(LocalDateTime.class)))
                .thenReturn(List.of(jobWithKeywords));

        scrapeScheduler.processPendingJobs();

        // performScrape called with jobId
        verify(scrapeServiceImpl).performScrape("job-1");

        // After merge, job should have keywords populated
        assert jobWithUrls.getKeywords().contains("technology");
    }

    @Test
    void shouldNotCallPerformScrapeWhenNoPendingJobs() {
        when(scrapeJobRepository.findPendingJobsDueWithUrls(
                any(LocalDateTime.class)))
                .thenReturn(List.of());

        scrapeScheduler.processPendingJobs();

        verify(scrapeServiceImpl, never()).performScrape(anyString());
        // Second query not even called if first returns empty
        verify(scrapeJobRepository, never())
                .findPendingJobsDueWithKeywords(any());
    }

    @Test
    void shouldContinueProcessingOtherJobsWhenOneFails() {
        ScrapeJob job1WithUrls = ScrapeJob.builder()
                .jobId("job-1")
                .urls(new ArrayList<>(List.of("https://a.com")))
                .keywords(new ArrayList<>())
                .status("PENDING")
                .scheduledAt(LocalDateTime.now().minusMinutes(1))
                .build();

        ScrapeJob job2WithUrls = ScrapeJob.builder()
                .jobId("job-2")
                .urls(new ArrayList<>(List.of("https://b.com")))
                .keywords(new ArrayList<>())
                .status("PENDING")
                .scheduledAt(LocalDateTime.now().minusMinutes(1))
                .build();

        ScrapeJob job1WithKeywords = ScrapeJob.builder()
                .jobId("job-1")
                .urls(new ArrayList<>())
                .keywords(new ArrayList<>(List.of("tech")))
                .status("PENDING")
                .scheduledAt(LocalDateTime.now().minusMinutes(1))
                .build();

        ScrapeJob job2WithKeywords = ScrapeJob.builder()
                .jobId("job-2")
                .urls(new ArrayList<>())
                .keywords(new ArrayList<>(List.of("science")))
                .status("PENDING")
                .scheduledAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(scrapeJobRepository.findPendingJobsDueWithUrls(
                any(LocalDateTime.class)))
                .thenReturn(List.of(job1WithUrls, job2WithUrls));
        when(scrapeJobRepository.findPendingJobsDueWithKeywords(
                any(LocalDateTime.class)))
                .thenReturn(List.of(job1WithKeywords, job2WithKeywords));

        // job-1 fails
        doThrow(new RuntimeException("Scrape failed"))
                .when(scrapeServiceImpl).performScrape("job-1");

        // Should not throw
        scrapeScheduler.processPendingJobs();

        // Both attempted despite job-1 failing
        verify(scrapeServiceImpl).performScrape("job-1");
        verify(scrapeServiceImpl).performScrape("job-2");
    }

    @Test
    void shouldProcessMultiplePendingJobs() {
        ScrapeJob job1 = ScrapeJob.builder()
                .jobId("job-1").status("PENDING")
                .urls(new ArrayList<>(List.of("https://a.com")))
                .keywords(new ArrayList<>())
                .scheduledAt(LocalDateTime.now().minusMinutes(1))
                .build();

        ScrapeJob job2 = ScrapeJob.builder()
                .jobId("job-2").status("PENDING")
                .urls(new ArrayList<>(List.of("https://b.com")))
                .keywords(new ArrayList<>())
                .scheduledAt(LocalDateTime.now().minusMinutes(2))
                .build();

        when(scrapeJobRepository.findPendingJobsDueWithUrls(
                any(LocalDateTime.class)))
                .thenReturn(List.of(job1, job2));
        when(scrapeJobRepository.findPendingJobsDueWithKeywords(
                any(LocalDateTime.class)))
                .thenReturn(List.of());

        scrapeScheduler.processPendingJobs();

        verify(scrapeServiceImpl).performScrape("job-1");
        verify(scrapeServiceImpl).performScrape("job-2");
    }
}
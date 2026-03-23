package com.scraper.triescraper.service;

import com.scraper.triescraper.dto.request.ScrapeRequest;
import com.scraper.triescraper.dto.request.SearchRequest;
import com.scraper.triescraper.dto.response.JobStatusResponse;
import com.scraper.triescraper.dto.response.ScrapeResponse;
import com.scraper.triescraper.dto.response.SearchResponse;
import com.scraper.triescraper.model.entity.ScrapedData;
import com.scraper.triescraper.model.entity.ScrapeJob;
import com.scraper.triescraper.repository.ScrapedDataRepository;
import com.scraper.triescraper.repository.ScrapeJobRepository;
import com.scraper.triescraper.service.impl.ScrapeJobPersistenceService;
import com.scraper.triescraper.service.impl.ScrapeServiceImpl;
import com.scraper.triescraper.trie.Trie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScrapeServiceImplTest {

    @Mock
    private ScrapedDataRepository scrapedDataRepository;

    @Mock
    private ScrapeJobRepository scrapeJobRepository;

    @Mock
    private Trie trie;

    @Mock
    private ScrapeJobPersistenceService persistenceService;

    @InjectMocks
    private ScrapeServiceImpl scrapeServiceImpl;

    private ScrapeRequest scrapeRequest;
    private SearchRequest searchRequest;
    private ScrapedData mockScrapedData;
    private ScrapeJob mockJob;

    @BeforeEach
    void setUp() {
        scrapeRequest = new ScrapeRequest();
        scrapeRequest.setUrls(new ArrayList<>(
                List.of("https://example.com")));
        scrapeRequest.setKeywords(new ArrayList<>(
                List.of("technology")));

        searchRequest = new SearchRequest();
        searchRequest.setPrefix("tech");
        searchRequest.setLimit(5);

        mockScrapedData = ScrapedData.builder()
                .id("id-001")
                .jobId("test-job-id")
                .sourceUrl("https://example.com")
                .matchedContent("Latest technology trends...")
                .keyword("technology")
                .scrapedAt(LocalDateTime.now())
                .build();

        mockJob = ScrapeJob.builder()
                .jobId("test-job-id")
                .urls(new ArrayList<>(List.of("https://example.com")))
                .keywords(new ArrayList<>(List.of("technology")))
                .status("IN_PROGRESS")
                .scheduledAt(LocalDateTime.now())
                .build();
    }

    // ─── initiateScrape tests ────────────────────────────────────────

    @Test
    void shouldInitiateScrapeAndReturnSuccess() {
        when(scrapeJobRepository.save(any(ScrapeJob.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(persistenceService.updateJobStatus(
                anyString(), eq("IN_PROGRESS")))
                .thenReturn(mockJob);

        ScrapeResponse response =
                scrapeServiceImpl.initiateScrape(scrapeRequest);

        assertEquals("success", response.getStatus());
        assertEquals("Scraping initiated successfully.",
                response.getMessage());
        assertNotNull(response.getJobId());
    }

    @Test
    void shouldGenerateUniqueJobIdForEachRequest() {
        when(scrapeJobRepository.save(any(ScrapeJob.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(persistenceService.updateJobStatus(
                anyString(), eq("IN_PROGRESS")))
                .thenReturn(mockJob);

        ScrapeResponse r1 =
                scrapeServiceImpl.initiateScrape(scrapeRequest);
        ScrapeResponse r2 =
                scrapeServiceImpl.initiateScrape(scrapeRequest);

        assertNotEquals(r1.getJobId(), r2.getJobId());
    }

    // ─── SELF-INVOCATION BUG FIX VERIFICATION ───────────────────────
    // Verifies performScrape() uses persistenceService (external bean)
    // NOT self-invocation (this.updateJobStatus())

    @Test
    void shouldNotCallPerformScrapeForFutureSchedule() {
        scrapeRequest.setSchedule(LocalDateTime.now().plusHours(2));

        when(scrapeJobRepository.save(any(ScrapeJob.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        scrapeServiceImpl.initiateScrape(scrapeRequest);

        // persistenceService.updateJobStatus() is called inside
        // performScrape() — if future schedule, must NOT be called
        verify(persistenceService, never())
                .updateJobStatus(anyString(), anyString());
    }

    @Test
    void shouldCallPersistenceServiceForImmediateScrape() {
        when(scrapeJobRepository.save(any(ScrapeJob.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(persistenceService.updateJobStatus(
                anyString(), eq("IN_PROGRESS")))
                .thenReturn(mockJob);

        scrapeServiceImpl.initiateScrape(scrapeRequest);

        // Proves performScrape() called persistenceService
        // NOT self-invocation
        verify(persistenceService).updateJobStatus(
                anyString(), eq("IN_PROGRESS"));
    }

    @Test
    void shouldCallSaveAndIndexAfterScraping() {
        when(scrapeJobRepository.save(any(ScrapeJob.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(persistenceService.updateJobStatus(
                anyString(), eq("IN_PROGRESS")))
                .thenReturn(mockJob);

        scrapeServiceImpl.initiateScrape(scrapeRequest);

        // Proves saveScrapedDataAndIndexTrie called on
        // external bean — not self-invocation
        verify(persistenceService).saveScrapedDataAndIndexTrie(
                anyString(), anyList());
    }

    @Test
    void shouldCallUpdateJobStatusCompletedAfterScraping() {
        when(scrapeJobRepository.save(any(ScrapeJob.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(persistenceService.updateJobStatus(
                anyString(), eq("IN_PROGRESS")))
                .thenReturn(mockJob);

        scrapeServiceImpl.initiateScrape(scrapeRequest);

        // Proves updateJobStatusCompleted called on
        // external bean — not self-invocation
        verify(persistenceService).updateJobStatusCompleted(
                anyString(), anyLong());
    }

    @Test
    void shouldAbortScrapeIfJobNotFound() {
        when(scrapeJobRepository.save(any(ScrapeJob.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // updateJobStatus returns null — job not found
        when(persistenceService.updateJobStatus(
                anyString(), eq("IN_PROGRESS")))
                .thenReturn(null);

        // Should not throw
        assertDoesNotThrow(() ->
                scrapeServiceImpl.initiateScrape(scrapeRequest));

        // saveScrapedDataAndIndexTrie must NOT be called
        // if job was not found
        verify(persistenceService, never())
                .saveScrapedDataAndIndexTrie(anyString(), anyList());
    }

    // ─── search tests ────────────────────────────────────────────────

    @Test
    void shouldReturnSearchResultsFromTrieIds() {
        when(trie.getScrapedDataIds("tech", 5))
                .thenReturn(List.of("id-001"));
        when(scrapedDataRepository.findAllById(List.of("id-001")))
                .thenReturn(List.of(mockScrapedData));

        SearchResponse response =
                scrapeServiceImpl.search(searchRequest);

        assertEquals("success", response.getStatus());
        assertEquals(1, response.getResults().size());
        assertEquals("https://example.com",
                response.getResults().get(0).getUrl());
        verify(trie).getScrapedDataIds("tech", 5);
        verify(scrapedDataRepository).findAllById(List.of("id-001"));
        verify(scrapedDataRepository, never())
                .findByKeywordStartingWithIgnoreCase(anyString());
    }

    @Test
    void shouldFallbackToDbWhenTrieReturnsNoIds() {
        when(trie.getScrapedDataIds("tech", 5))
                .thenReturn(List.of());
        when(scrapedDataRepository
                .findByKeywordStartingWithIgnoreCase("tech"))
                .thenReturn(List.of(mockScrapedData));

        SearchResponse response =
                scrapeServiceImpl.search(searchRequest);

        assertEquals("success", response.getStatus());
        assertEquals(1, response.getResults().size());
        verify(scrapedDataRepository)
                .findByKeywordStartingWithIgnoreCase("tech");
    }

    @Test
    void shouldReturnEmptyResultsWhenNothingFound() {
        when(trie.getScrapedDataIds("xyz", 5))
                .thenReturn(List.of());
        when(scrapedDataRepository
                .findByKeywordStartingWithIgnoreCase("xyz"))
                .thenReturn(List.of());

        searchRequest.setPrefix("xyz");
        SearchResponse response =
                scrapeServiceImpl.search(searchRequest);

        assertEquals("success", response.getStatus());
        assertTrue(response.getResults().isEmpty());
    }

    @Test
    void shouldDefaultLimitToFiveWhenZeroProvided() {
        searchRequest.setLimit(0);
        when(trie.getScrapedDataIds("tech", 5))
                .thenReturn(List.of());
        when(scrapedDataRepository
                .findByKeywordStartingWithIgnoreCase("tech"))
                .thenReturn(List.of());

        scrapeServiceImpl.search(searchRequest);

        verify(trie).getScrapedDataIds("tech", 5);
    }

    @Test
    void shouldNormalizePrefixToLowerCase() {
        searchRequest.setPrefix("TECH");
        when(trie.getScrapedDataIds("tech", 5))
                .thenReturn(List.of());
        when(scrapedDataRepository
                .findByKeywordStartingWithIgnoreCase("tech"))
                .thenReturn(List.of());

        scrapeServiceImpl.search(searchRequest);

        verify(trie).getScrapedDataIds("tech", 5);
    }

    // ─── getJobStatus tests ──────────────────────────────────────────

    @Test
    void shouldReturnNotFoundForInvalidJobId() {
        when(scrapeJobRepository.findByJobId("invalid"))
                .thenReturn(null);

        JobStatusResponse response =
                scrapeServiceImpl.getJobStatus("invalid");

        assertEquals("NOT_FOUND", response.getStatus());
        assertEquals("invalid", response.getJobId());
    }

    @Test
    void shouldReturnAllJobFieldsForValidJobId() {
        ScrapeJob job = ScrapeJob.builder()
                .jobId("test-job-id")
                .urls(new ArrayList<>(List.of(
                        "https://example.com",
                        "https://example2.com")))
                .keywords(new ArrayList<>(List.of(
                        "technology", "innovation")))
                .status("COMPLETED")
                .finishedAt(LocalDateTime.now())
                .dataSize("2 KB")
                .build();

        when(scrapeJobRepository.findByJobId("test-job-id"))
                .thenReturn(job);

        JobStatusResponse response =
                scrapeServiceImpl.getJobStatus("test-job-id");

        assertEquals("COMPLETED", response.getStatus());
        assertEquals("2 KB", response.getDataSize());
        assertEquals(2, response.getUrlsScraped().size());
        assertEquals(2, response.getKeywordsFound().size());
        assertNotNull(response.getFinishedAt());
    }

    @Test
    void shouldReturnPendingStatusForScheduledJob() {
        ScrapeJob job = ScrapeJob.builder()
                .jobId("test-job-id")
                .urls(new ArrayList<>(
                        List.of("https://example.com")))
                .keywords(new ArrayList<>(List.of("technology")))
                .status("PENDING")
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .build();

        when(scrapeJobRepository.findByJobId("test-job-id"))
                .thenReturn(job);

        JobStatusResponse response =
                scrapeServiceImpl.getJobStatus("test-job-id");

        assertEquals("PENDING", response.getStatus());
        assertNull(response.getFinishedAt());
    }
}
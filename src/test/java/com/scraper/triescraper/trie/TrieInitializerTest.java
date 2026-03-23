package com.scraper.triescraper.trie;

import com.scraper.triescraper.model.entity.ScrapedData;
import com.scraper.triescraper.repository.ScrapedDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrieInitializerTest {

    @Mock
    private Trie trie;

    @Mock
    private ScrapedDataRepository scrapedDataRepository;

    @InjectMocks
    private TrieInitializer trieInitializer;

    // ─── Startup rebuild verification ────────────────────────────────

    @Test
    void shouldInsertAllKeywordsAndIdsOnStartup() {
        ScrapedData data1 = ScrapedData.builder()
                .id("id-001")
                .keyword("technology")
                .sourceUrl("https://example.com")
                .matchedContent("tech content")
                .scrapedAt(LocalDateTime.now())
                .build();

        ScrapedData data2 = ScrapedData.builder()
                .id("id-002")
                .keyword("innovation")
                .sourceUrl("https://example2.com")
                .matchedContent("innovation content")
                .scrapedAt(LocalDateTime.now())
                .build();

        when(scrapedDataRepository.findAll())
                .thenReturn(List.of(data1, data2));

        trieInitializer.initializeTrie();

        // Both keywords inserted with their DB IDs
        verify(trie).insert("technology", "id-001");
        verify(trie).insert("innovation", "id-002");
    }

    @Test
    void shouldNotInsertAnythingWhenDbIsEmpty() {
        when(scrapedDataRepository.findAll()).thenReturn(List.of());

        trieInitializer.initializeTrie();

        // Nothing to insert — Trie must stay empty
        verify(trie, never()).insert(anyString(), anyString());
    }

    @Test
    void shouldCallFindAllOnStartup() {
        when(scrapedDataRepository.findAll()).thenReturn(List.of());

        trieInitializer.initializeTrie();

        // Must load all data from DB on startup
        verify(scrapedDataRepository).findAll();
    }

    @Test
    void shouldInsertCorrectIdForEachKeyword() {
        // Same keyword, two different DB rows (different pages)
        ScrapedData data1 = ScrapedData.builder()
                .id("id-001")
                .keyword("technology")
                .sourceUrl("https://page1.com")
                .matchedContent("content 1")
                .scrapedAt(LocalDateTime.now())
                .build();

        ScrapedData data2 = ScrapedData.builder()
                .id("id-002")
                .keyword("technology") // same keyword
                .sourceUrl("https://page2.com")
                .matchedContent("content 2")
                .scrapedAt(LocalDateTime.now())
                .build();

        when(scrapedDataRepository.findAll())
                .thenReturn(List.of(data1, data2));

        trieInitializer.initializeTrie();

        // Same keyword inserted TWICE with different IDs
        verify(trie).insert("technology", "id-001");
        verify(trie).insert("technology", "id-002");
        verify(trie, times(2)).insert(eq("technology"), anyString());
    }

    @Test
    void shouldHandleLargeNumberOfEntries() {
        // Simulate 1000 DB rows — startup must handle all of them
        List<ScrapedData> largeList = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            largeList.add(ScrapedData.builder()
                    .id("id-" + i)
                    .keyword("keyword-" + i)
                    .sourceUrl("https://example.com/" + i)
                    .matchedContent("content " + i)
                    .scrapedAt(LocalDateTime.now())
                    .build());
        }

        when(scrapedDataRepository.findAll()).thenReturn(largeList);

        trieInitializer.initializeTrie();

        verify(trie, times(1000)).insert(anyString(), anyString());
    }
}
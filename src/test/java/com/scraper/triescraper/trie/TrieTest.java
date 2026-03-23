package com.scraper.triescraper.trie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrieTest {

    private Trie trie;

    @BeforeEach
    void setUp() {
        trie = new Trie();
    }

    // ─── insert + search ────────────────────────────────────────────

    @Test
    void shouldInsertAndSearchWord() {
        trie.insert("technology", "id-001");
        assertTrue(trie.search("technology"));
    }

    @Test
    void shouldReturnFalseForNonExistentWord() {
        assertFalse(trie.search("technology"));
    }

    // ─── startsWith ─────────────────────────────────────────────────

    @Test
    void shouldReturnTrueForExistingPrefix() {
        trie.insert("technology", "id-001");
        assertTrue(trie.startsWith("tech"));
    }

    @Test
    void shouldReturnFalseForNonExistentPrefix() {
        assertFalse(trie.startsWith("xyz"));
    }

    // ─── getWordsWithPrefix ──────────────────────────────────────────

    @Test
    void shouldReturnWordsWithMatchingPrefix() {
        trie.insert("technology", "id-001");
        trie.insert("technical",  "id-002");
        trie.insert("technique",  "id-003");
        trie.insert("innovation", "id-004");

        List<String> results = trie.getWordsWithPrefix("tech", 10);

        assertEquals(3, results.size());
        assertTrue(results.contains("technology"));
        assertTrue(results.contains("technical"));
        assertTrue(results.contains("technique"));
        assertFalse(results.contains("innovation")); // ← wrong prefix
    }

    @Test
    void shouldRespectLimitInPrefixSearch() {
        trie.insert("technology", "id-001");
        trie.insert("technical",  "id-002");
        trie.insert("technique",  "id-003");

        List<String> results = trie.getWordsWithPrefix("tech", 2);

        assertEquals(2, results.size()); // ← limit respected
    }

    // ─── getScrapedDataIds ───────────────────────────────────────────

    @Test
    void shouldReturnCorrectIdsForPrefix() {
        trie.insert("technology", "id-001");
        trie.insert("technical",  "id-002");
        trie.insert("technique",  "id-003");
        trie.insert("innovation", "id-004"); // ← different prefix

        List<String> ids = trie.getScrapedDataIds("tech", 10);

        assertEquals(3, ids.size());
        assertTrue(ids.contains("id-001"));
        assertTrue(ids.contains("id-002"));
        assertTrue(ids.contains("id-003"));
        assertFalse(ids.contains("id-004")); // ← should NOT be included
    }

    @Test
    void shouldRespectLimitInGetScrapedDataIds() {
        trie.insert("technology", "id-001");
        trie.insert("technical",  "id-002");
        trie.insert("technique",  "id-003");

        List<String> ids = trie.getScrapedDataIds("tech", 2);

        assertEquals(2, ids.size()); // ← limit respected
    }

    @Test
    void shouldReturnEmptyIdsForNonExistentPrefix() {
        trie.insert("technology", "id-001");

        List<String> ids = trie.getScrapedDataIds("xyz", 10);

        assertTrue(ids.isEmpty());
    }

    @Test
    void shouldReturnIdForExactWordMatch() {
        trie.insert("technology", "id-001");

        List<String> ids = trie.getScrapedDataIds("technology", 10);

        assertEquals(1, ids.size());
        assertEquals("id-001", ids.get(0));
    }

    @Test
    void shouldStorMultipleIdsForSameKeyword() {
        // Same keyword scraped from two different pages
        trie.insert("technology", "id-001");
        trie.insert("technology", "id-002");

        List<String> ids = trie.getScrapedDataIds("technology", 10);

        assertEquals(2, ids.size());
        assertTrue(ids.contains("id-001"));
        assertTrue(ids.contains("id-002"));
    }

    @Test
    void shouldNotStoreDuplicateIds() {
        // Same ID inserted twice — should only appear once
        trie.insert("technology", "id-001");
        trie.insert("technology", "id-001");

        List<String> ids = trie.getScrapedDataIds("technology", 10);

        assertEquals(1, ids.size());
        assertEquals("id-001", ids.get(0));
    }

    // ─── edge cases ──────────────────────────────────────────────────

    @Test
    void shouldHandleNullInsertGracefully() {
        assertDoesNotThrow(() -> trie.insert(null, "id-001"));
    }

    @Test
    void shouldHandleBlankInsertGracefully() {
        assertDoesNotThrow(() -> trie.insert("   ", "id-001"));
    }

    @Test
    void shouldHandleNullIdGracefully() {
        // null ID should not be added to the list
        assertDoesNotThrow(() -> trie.insert("technology", null));

        List<String> ids = trie.getScrapedDataIds("technology", 10);
        assertTrue(ids.isEmpty()); // ← null ID was rejected
    }

    @Test
    void shouldClearTrie() {
        trie.insert("technology", "id-001");
        trie.clear();

        assertFalse(trie.search("technology"));
        assertTrue(trie.getScrapedDataIds("tech", 10).isEmpty());
    }

    @Test
    void shouldBeCaseInsensitive() {
        trie.insert("Technology", "id-001"); // ← uppercase inserted

        assertTrue(trie.search("technology"));  // ← lowercase search works
        assertTrue(trie.startsWith("tech"));
        assertFalse(trie.getScrapedDataIds("tech", 10).isEmpty());
    }
}
package com.scraper.triescraper.trie;

import com.scraper.triescraper.repository.ScrapedDataRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrieInitializer {

    private final Trie trie;
    private final ScrapedDataRepository scrapedDataRepository;

    // Runs ONCE automatically after Spring context is fully loaded
    @PostConstruct
    public void initializeTrie() {
        log.info("Initializing Trie from database...");

        var allData = scrapedDataRepository.findAll();

        if (allData.isEmpty()) {
            log.info("No scraped data found in database. Trie is empty.");
            return;
        }

        allData.forEach(data -> trie.insert(data.getKeyword(), data.getId()));

        log.info("Trie successfully initialized with {} entries.", allData.size());
    }
}
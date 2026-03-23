package com.scraper.triescraper.trie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrieNode {

    private final Map<Character, TrieNode> children;
    private boolean endOfWord;
    private final List<String> scrapedDataIds; // ← direct pointers to DB rows

    public TrieNode() {
        this.children = new HashMap<>();
        this.endOfWord = false;
        this.scrapedDataIds = new ArrayList<>();
    }

    public Map<Character, TrieNode> getChildren() {
        return children;
    }

    public boolean isEndOfWord() {
        return endOfWord;
    }

    public void setEndOfWord(boolean endOfWord) {
        this.endOfWord = endOfWord;
    }

    public List<String> getScrapedDataIds() {
        return scrapedDataIds;
    }

    // Adds a DB row ID to this node — called when a keyword is inserted
    public void addScrapedDataId(String id) {
        if (id != null && !scrapedDataIds.contains(id)) {
            scrapedDataIds.add(id);
        }
    }
}
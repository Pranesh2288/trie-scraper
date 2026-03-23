package com.scraper.triescraper.trie;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class Trie {

    private final TrieNode root;

    public Trie() {
        this.root = new TrieNode();
    }

    // Insert a word with its associated DB row ID
    public void insert(String word, String scrapedDataId) {
        if (word == null || word.isBlank()) return;
        TrieNode current = root;
        for (char ch : word.toLowerCase().toCharArray()) {
            current.getChildren()
                   .putIfAbsent(ch, new TrieNode());
            current = current.getChildren().get(ch);
        }
        current.setEndOfWord(true);
        current.addScrapedDataId(scrapedDataId); // ← store DB row ID at end node
    }

    // Check if exact word exists
    public boolean search(String word) {
        if (word == null || word.isBlank()) return false;
        TrieNode node = getNode(word.toLowerCase());
        return node != null && node.isEndOfWord();
    }

    // Check if any word starts with given prefix
    public boolean startsWith(String prefix) {
        if (prefix == null || prefix.isBlank()) return false;
        return getNode(prefix.toLowerCase()) != null;
    }

    // Get all words with given prefix (autocomplete)
    public List<String> getWordsWithPrefix(String prefix, int limit) {
        List<String> results = new ArrayList<>();
        if (prefix == null || prefix.isBlank()) return results;

        TrieNode node = getNode(prefix.toLowerCase());
        if (node == null) return results;

        collectWords(node, new StringBuilder(prefix.toLowerCase()), results, limit);
        return results;
    }

    // Get all DB row IDs for keywords matching the given prefix
    public List<String> getScrapedDataIds(String prefix, int limit) {
        List<String> ids = new ArrayList<>();
        if (prefix == null || prefix.isBlank()) return ids;

        TrieNode node = getNode(prefix.toLowerCase());
        if (node == null) return ids;

        collectIds(node, ids, limit); // ← DFS to collect all IDs under prefix node
        return ids;
    }

    // Navigate to the node at the end of the prefix
    private TrieNode getNode(String prefix) {
        TrieNode current = root;
        for (char ch : prefix.toCharArray()) {
            if (!current.getChildren().containsKey(ch)) return null;
            current = current.getChildren().get(ch);
        }
        return current;
    }

    // DFS to collect all words from a given node
    private void collectWords(TrieNode node, StringBuilder current,
                               List<String> results, int limit) {
        if (results.size() >= limit) return;
        if (node.isEndOfWord()) results.add(current.toString());

        for (Map.Entry<Character, TrieNode> entry : node.getChildren().entrySet()) {
            current.append(entry.getKey());
            collectWords(entry.getValue(), current, results, limit);
            current.deleteCharAt(current.length() - 1);
        }
    }

    // DFS to collect all DB row IDs from a given node downward
    private void collectIds(TrieNode node, List<String> ids, int limit) {
        if (ids.size() >= limit) return;

        // If this node marks end of a word, collect its IDs
        if (node.isEndOfWord()) {
            for (String id : node.getScrapedDataIds()) {
                if (!ids.contains(id) && ids.size() < limit) {
                    ids.add(id);
                }
            }
        }

        // Recurse into all children
        for (TrieNode child : node.getChildren().values()) {
            if (ids.size() >= limit) return;
            collectIds(child, ids, limit);
        }
    }

    // Clear the entire trie
    public void clear() {
        root.getChildren().clear();
    }
}
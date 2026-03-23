package com.scraper.triescraper.repository;

import com.scraper.triescraper.model.entity.ScrapedData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScrapedDataRepository extends JpaRepository<ScrapedData, String> {

    List<ScrapedData> findByKeywordStartingWithIgnoreCase(String prefix);

    List<ScrapedData> findByJobId(String jobId);
}
package com.scraper.triescraper.repository;

import com.scraper.triescraper.model.entity.ScrapeJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScrapeJobRepository extends JpaRepository<ScrapeJob, String> {

    ScrapeJob findByJobId(String jobId);

    // Step 1: fetch jobs with URLs only
    @Query("SELECT DISTINCT j FROM ScrapeJob j " +
           "LEFT JOIN FETCH j.urls " +
           "WHERE j.status = 'PENDING' " +
           "AND j.scheduledAt <= :now")
    List<ScrapeJob> findPendingJobsDueWithUrls(
            @Param("now") LocalDateTime now);

    // Step 2: fetch jobs with keywords only
    @Query("SELECT DISTINCT j FROM ScrapeJob j " +
           "LEFT JOIN FETCH j.keywords " +
           "WHERE j.status = 'PENDING' " +
           "AND j.scheduledAt <= :now")
    List<ScrapeJob> findPendingJobsDueWithKeywords(
            @Param("now") LocalDateTime now);
}
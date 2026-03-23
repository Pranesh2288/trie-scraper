package com.scraper.triescraper.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "scrape_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapeJob {

    @Id
    private String jobId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_urls", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "url", length = 2048)
    private List<String> urls;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_keywords", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "keyword")
    private List<String> keywords;

    @Column(nullable = false)
    private String status; // PENDING, IN_PROGRESS, COMPLETED, FAILED

    private LocalDateTime scheduledAt;
    private LocalDateTime finishedAt;
    private String dataSize;
}
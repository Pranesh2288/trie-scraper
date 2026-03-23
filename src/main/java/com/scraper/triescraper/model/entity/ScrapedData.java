package com.scraper.triescraper.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scraped_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapedData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String jobId;

    @Column(nullable = false, length = 2048)
    private String sourceUrl;

    @Column(columnDefinition = "TEXT")
    private String matchedContent;

    @Column(nullable = false)
    private String keyword;

    @Column(nullable = false)
    private LocalDateTime scrapedAt;
}
package dev.saberlabs.give_me_the_code.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDate;

/**
 * LeetCode's own daily coding challenge, persisted once per calendar date rather than fetched
 * live on every request: {@code challengeDate} is the thing that is actually unique here, not
 * {@code titleSlug}, since the same problem could in principle be reused as the daily challenge
 * again on a different date.
 */
@Entity
@Table(name = "challenges")
@Getter
@Setter
@ToString
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String leetcodeQuestionId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String titleSlug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(nullable = false)
    private String link;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false, unique = true)
    private LocalDate challengeDate;

    @Column(nullable = false)
    private Instant fetchedAt;

    protected Challenge() {
        // JPA
    }

    public Challenge(String leetcodeQuestionId, String title, String titleSlug, Difficulty difficulty,
                      String link, String content, LocalDate challengeDate) {
        this.leetcodeQuestionId = leetcodeQuestionId;
        this.title = title;
        this.titleSlug = titleSlug;
        this.difficulty = difficulty;
        this.link = link;
        this.content = content;
        this.challengeDate = challengeDate;
        this.fetchedAt = Instant.now();
    }
}

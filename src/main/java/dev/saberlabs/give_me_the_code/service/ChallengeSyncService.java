package dev.saberlabs.give_me_the_code.service;

import dev.saberlabs.give_me_the_code.client.LeetCodeClient;
import dev.saberlabs.give_me_the_code.client.LeetCodeDailyChallenge;
import dev.saberlabs.give_me_the_code.model.Challenge;
import dev.saberlabs.give_me_the_code.model.Difficulty;
import dev.saberlabs.give_me_the_code.repository.ChallengeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Keeps one {@link Challenge} row per UTC calendar date, the same shape LeetCode itself uses for
 * "the" daily challenge. A {@code @Scheduled} job fetches it once a day; every other read just
 * hits the database, which both satisfies the persistence requirement and keeps this app from
 * calling LeetCode's rate-limited endpoint on every request.
 */
@Service
public class ChallengeSyncService {

    private static final Logger log = LoggerFactory.getLogger(ChallengeSyncService.class);
    private static final String LEETCODE_BASE_URL = "https://leetcode.com";

    private final ChallengeRepository challengeRepository;
    private final LeetCodeClient leetCodeClient;

    public ChallengeSyncService(ChallengeRepository challengeRepository, LeetCodeClient leetCodeClient) {
        this.challengeRepository = challengeRepository;
        this.leetCodeClient = leetCodeClient;
    }

    /** Runs once a day, five minutes after LeetCode's own midnight-UTC rollover. */
    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    public void refreshDailyChallenge() {
        try {
            fetchAndPersist(todayUtc());
        } catch (Exception e) {
            log.warn("Scheduled LeetCode refresh failed; a request-time fallback or the next "
                    + "scheduled run will retry", e);
        }
    }

    /** The synchronous path: today's row if already cached, otherwise fetch-and-store right now. */
    @Transactional
    public Challenge getOrFetchToday() {
        LocalDate today = todayUtc();
        return challengeRepository.findByChallengeDate(today)
                .orElseGet(() -> fetchAndPersist(today));
    }

    /**
     * The async path a controller should actually call: identical result, but frees the request
     * thread while the (rare) cache-miss LeetCode call is in flight.
     */
    @Async("leetCodeTaskExecutor")
    public CompletableFuture<Challenge> getOrFetchTodayAsync() {
        return CompletableFuture.completedFuture(getOrFetchToday());
    }

    private Challenge fetchAndPersist(LocalDate date) {
        LeetCodeDailyChallenge daily = leetCodeClient.fetchDailyChallenge();
        String content = leetCodeClient.fetchQuestionContent(daily.question().titleSlug());
        Difficulty difficulty = parseDifficulty(daily.question().difficulty());
        String link = absoluteLink(daily.link());

        Challenge challenge = challengeRepository.findByChallengeDate(date).orElse(null);
        if (challenge == null) {
            challenge = new Challenge(daily.question().questionFrontendId(), daily.question().title(),
                    daily.question().titleSlug(), difficulty, link, content, date);
        } else {
            challenge.setLeetcodeQuestionId(daily.question().questionFrontendId());
            challenge.setTitle(daily.question().title());
            challenge.setTitleSlug(daily.question().titleSlug());
            challenge.setDifficulty(difficulty);
            challenge.setLink(link);
            challenge.setContent(content);
            challenge.setFetchedAt(Instant.now());
        }
        return challengeRepository.save(challenge);
    }

    private Difficulty parseDifficulty(String raw) {
        return Difficulty.valueOf(raw.toUpperCase(Locale.ROOT));
    }

    private String absoluteLink(String link) {
        return link.startsWith("http") ? link : LEETCODE_BASE_URL + link;
    }

    private LocalDate todayUtc() {
        return LocalDate.now(ZoneOffset.UTC);
    }
}

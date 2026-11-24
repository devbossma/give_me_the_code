package dev.saberlabs.give_me_the_code.client;

import org.springframework.graphql.client.GraphQlClientException;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

/**
 * Talks to LeetCode's real GraphQL endpoint, blocking (via {@code retrieveSync}) because every
 * caller here already runs off the servlet request thread: the daily {@code @Scheduled} refresh
 * runs on its own scheduler thread, and {@code ChallengeSyncService}'s on-demand fallback wraps
 * this in {@code @Async} itself rather than mixing two different asynchronous models together.
 *
 * <p>{@code @Retryable} covers transient failures (a 429, a brief 5xx) with backoff before giving
 * up; the caller decides what to do once retries are exhausted (typically: serve a stale cached
 * value rather than fail the whole request).
 */
@Component
public class LeetCodeGraphQlClient implements LeetCodeClient {

    private static final String DAILY_CHALLENGE_QUERY = """
            query questionOfToday {
              activeDailyCodingChallengeQuestion {
                date
                link
                question {
                  questionFrontendId
                  title
                  titleSlug
                  difficulty
                }
              }
            }
            """;

    private static final String QUESTION_CONTENT_QUERY = """
            query questionContent($titleSlug: String!) {
              question(titleSlug: $titleSlug) {
                content
              }
            }
            """;

    private static final String RECENT_AC_SUBMISSIONS_QUERY = """
            query recentAcSubmissions($username: String!, $limit: Int!) {
              recentAcSubmissionList(username: $username, limit: $limit) {
                titleSlug
              }
            }
            """;

    private final HttpGraphQlClient graphQlClient;

    public LeetCodeGraphQlClient(HttpGraphQlClient leetCodeGraphQlClient) {
        this.graphQlClient = leetCodeGraphQlClient;
    }

    @Override
    @Retryable(retryFor = {WebClientResponseException.class, GraphQlClientException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public LeetCodeDailyChallenge fetchDailyChallenge() {
        return graphQlClient.document(DAILY_CHALLENGE_QUERY)
                .retrieveSync("activeDailyCodingChallengeQuestion")
                .toEntity(LeetCodeDailyChallenge.class);
    }

    @Override
    @Retryable(retryFor = {WebClientResponseException.class, GraphQlClientException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public String fetchQuestionContent(String titleSlug) {
        return graphQlClient.document(QUESTION_CONTENT_QUERY)
                .variable("titleSlug", titleSlug)
                .retrieveSync("question.content")
                .toEntity(String.class);
    }

    @Override
    @Retryable(retryFor = {WebClientResponseException.class, GraphQlClientException.class}, maxAttempts = 3,
            backoff = @Backoff(delay = 500, multiplier = 2))
    public List<String> fetchRecentAcSubmissionSlugs(String leetcodeUsername, int limit) {
        return graphQlClient.document(RECENT_AC_SUBMISSIONS_QUERY)
                .variable("username", leetcodeUsername)
                .variable("limit", limit)
                .retrieveSync("recentAcSubmissionList")
                .toEntityList(RecentAcSubmission.class)
                .stream()
                .map(RecentAcSubmission::titleSlug)
                .toList();
    }
}

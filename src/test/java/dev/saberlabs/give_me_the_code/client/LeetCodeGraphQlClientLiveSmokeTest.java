package dev.saberlabs.give_me_the_code.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hits the real {@code leetcode.com/graphql} endpoint, not a stub. Skipped unless
 * {@code RUN_LIVE_LEETCODE_TESTS=true} is set, so {@code mvn verify} never depends on live
 * internet access or risks tripping LeetCode's own rate limiting. See the README for how to run
 * this on demand.
 */
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_LEETCODE_TESTS", matches = "true")
class LeetCodeGraphQlClientLiveSmokeTest {

    private static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    private LeetCodeGraphQlClient realClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl("https://leetcode.com/graphql")
                .defaultHeader(HttpHeaders.USER_AGENT, BROWSER_USER_AGENT)
                .defaultHeader(HttpHeaders.REFERER, "https://leetcode.com")
                .defaultHeader(HttpHeaders.ORIGIN, "https://leetcode.com")
                .build();
        return new LeetCodeGraphQlClient(HttpGraphQlClient.builder(webClient).build());
    }

    @Test
    void fetchesTodaysRealDailyChallengeAndItsContent() {
        LeetCodeGraphQlClient client = realClient();

        LeetCodeDailyChallenge daily = client.fetchDailyChallenge();
        assertThat(daily.question().titleSlug()).isNotBlank();
        assertThat(daily.question().difficulty()).isIn("Easy", "Medium", "Hard");

        String content = client.fetchQuestionContent(daily.question().titleSlug());
        assertThat(content).isNotBlank();
    }
}

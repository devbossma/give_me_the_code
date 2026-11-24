package dev.saberlabs.give_me_the_code.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Stubs the real {@code leetcode.com/graphql} endpoint with WireMock, so this suite never
 * depends on live internet access or risks getting rate-limited by the real service. A separate,
 * explicitly opt-in {@link LeetCodeGraphQlClientLiveSmokeTest} verifies the same queries against
 * the real endpoint.
 */
class LeetCodeGraphQlClientTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();

    @AfterEach
    void resetStubs() {
        wireMock.resetAll();
    }

    private LeetCodeGraphQlClient newClient() {
        WebClient webClient = WebClient.builder().baseUrl(wireMock.baseUrl()).build();
        HttpGraphQlClient graphQlClient = HttpGraphQlClient.builder(webClient).build();
        return new LeetCodeGraphQlClient(graphQlClient);
    }

    @Test
    void fetchDailyChallengeParsesTheRealResponseShape() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("questionOfToday"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("""
                        {
                          "data": {
                            "activeDailyCodingChallengeQuestion": {
                              "date": "2026-11-24",
                              "link": "/problems/two-sum/",
                              "question": {
                                "questionFrontendId": "1",
                                "title": "Two Sum",
                                "titleSlug": "two-sum",
                                "difficulty": "Easy"
                              }
                            }
                          }
                        }
                        """)));

        LeetCodeDailyChallenge daily = newClient().fetchDailyChallenge();

        assertThat(daily.date()).isEqualTo("2026-11-24");
        assertThat(daily.link()).isEqualTo("/problems/two-sum/");
        assertThat(daily.question().questionFrontendId()).isEqualTo("1");
        assertThat(daily.question().title()).isEqualTo("Two Sum");
        assertThat(daily.question().titleSlug()).isEqualTo("two-sum");
        assertThat(daily.question().difficulty()).isEqualTo("Easy");
    }

    @Test
    void fetchQuestionContentSendsTheTitleSlugVariable() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("questionContent"))
                .withRequestBody(containing("two-sum"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("""
                        {"data": {"question": {"content": "<p>Given an array...</p>"}}}
                        """)));

        String content = newClient().fetchQuestionContent("two-sum");

        assertThat(content).isEqualTo("<p>Given an array...</p>");
    }

    @Test
    void fetchRecentAcSubmissionSlugsReturnsJustTheSlugs() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("recentAcSubmissions"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("""
                        {"data": {"recentAcSubmissionList": [
                          {"titleSlug": "two-sum"},
                          {"titleSlug": "add-two-numbers"}
                        ]}}
                        """)));

        List<String> slugs = newClient().fetchRecentAcSubmissionSlugs("some-user", 2);

        assertThat(slugs).containsExactly("two-sum", "add-two-numbers");
    }

    @Test
    void aServerErrorPropagatesAsAWebClientResponseException() {
        wireMock.stubFor(post(urlEqualTo("/"))
                .withRequestBody(containing("questionOfToday"))
                .willReturn(aResponse().withStatus(500)));

        // This client was constructed directly (new, not through the Spring AOP proxy), so
        // @Retryable never engages here; see LeetCodeGraphQlClientRetryTest for retry behavior.
        assertThatThrownBy(() -> newClient().fetchDailyChallenge())
                .isInstanceOf(WebClientResponseException.class);
    }
}

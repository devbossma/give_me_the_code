package dev.saberlabs.give_me_the_code.client;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpGraphQlClient;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code @Retryable} only does anything when the bean is invoked through Spring's AOP proxy, so
 * this needs a real (if minimal) application context, unlike the plain {@code new ...(...)}
 * instantiation the rest of {@link LeetCodeGraphQlClientTest} uses.
 */
class LeetCodeGraphQlClientRetryTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().build();

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void cleanUp() {
        if (context != null) {
            context.close();
        }
        wireMock.resetAll();
    }

    @Configuration
    @EnableRetry
    static class RetryTestConfig {
        @Bean
        LeetCodeClient leetCodeClient() {
            WebClient webClient = WebClient.builder().baseUrl(wireMock.baseUrl()).build();
            return new LeetCodeGraphQlClient(HttpGraphQlClient.builder(webClient).build());
        }
    }

    private LeetCodeClient retryableClient() {
        context = new AnnotationConfigApplicationContext(RetryTestConfig.class);
        return context.getBean(LeetCodeClient.class);
    }

    @Test
    void recoversAfterTransientFailuresWithinTheRetryBudget() {
        wireMock.stubFor(post(urlEqualTo("/")).inScenario("flaky-leetcode")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second attempt"));
        wireMock.stubFor(post(urlEqualTo("/")).inScenario("flaky-leetcode")
                .whenScenarioStateIs("second attempt")
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("""
                        {"data": {"activeDailyCodingChallengeQuestion": {
                          "date": "2026-11-24", "link": "/problems/two-sum/",
                          "question": {"questionFrontendId": "1", "title": "Two Sum",
                                       "titleSlug": "two-sum", "difficulty": "Easy"}
                        }}}
                        """)));

        LeetCodeDailyChallenge daily = retryableClient().fetchDailyChallenge();

        assertThat(daily.question().titleSlug()).isEqualTo("two-sum");
    }

    @Test
    void givesUpAfterExhaustingTheRetryBudget() {
        wireMock.stubFor(post(urlEqualTo("/")).willReturn(aResponse().withStatus(503)));
        LeetCodeClient client = retryableClient();

        assertThatThrownBy(client::fetchDailyChallenge).isInstanceOf(WebClientResponseException.class);

        wireMock.verify(3, com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor(urlEqualTo("/")));
    }
}

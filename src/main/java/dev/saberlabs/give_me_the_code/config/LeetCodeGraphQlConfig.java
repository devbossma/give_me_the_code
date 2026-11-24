package dev.saberlabs.give_me_the_code.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Wires an {@link HttpGraphQlClient} aimed at LeetCode's own (undocumented) GraphQL endpoint.
 * {@code leetcode.graphql.base-url} points at the real endpoint by default and is overridden to
 * a WireMock stub's URL in tests, so the test suite never depends on live internet access.
 *
 * <p>The User-Agent/Referer/Origin headers below are not spoofing a login or bypassing any
 * authorization: {@code activeDailyCodingChallengeQuestion}, {@code question(titleSlug)}, and
 * {@code recentAcSubmissionList(username)} are all public, unauthenticated queries, the same
 * data an anonymous visitor's browser fetches. Without headers that look like an ordinary
 * browser request, LeetCode's own anti-bot filtering rejects the request outright, authenticated
 * or not; this is simply what it takes for the request to be answered at all.
 */
@Configuration
public class LeetCodeGraphQlConfig {

    private static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";

    @Value("${leetcode.graphql.base-url}")
    private String baseUrl;

    @Bean
    public HttpGraphQlClient leetCodeHttpGraphQlClient() {
        // Built directly rather than injecting Spring Boot's auto-configured WebClient.Builder:
        // that bean is only registered for a reactive-type application, not a servlet one, even
        // with spring-webflux on the classpath purely to supply WebClient itself.
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, BROWSER_USER_AGENT)
                .defaultHeader(HttpHeaders.REFERER, "https://leetcode.com")
                .defaultHeader(HttpHeaders.ORIGIN, "https://leetcode.com")
                .build();
        return HttpGraphQlClient.builder(webClient).build();
    }
}

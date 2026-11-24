package dev.saberlabs.give_me_the_code.client;

import java.util.List;

/**
 * What the rest of the app needs from LeetCode's GraphQL endpoint, independent of how it is
 * called. Blocking on purpose: every caller already runs off the servlet request thread (a
 * {@code @Scheduled} job, or a service method that itself wraps the call in {@code @Async}), so
 * mixing a reactive return type here on top of that would just be two asynchronous models
 * fighting each other for no benefit.
 */
public interface LeetCodeClient {

    LeetCodeDailyChallenge fetchDailyChallenge();

    String fetchQuestionContent(String titleSlug);

    List<String> fetchRecentAcSubmissionSlugs(String leetcodeUsername, int limit);
}

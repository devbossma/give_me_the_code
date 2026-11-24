package dev.saberlabs.give_me_the_code.client;

/**
 * The shape of {@code activeDailyCodingChallengeQuestion} exactly as LeetCode's GraphQL endpoint
 * returns it. Record component names are chosen to match the response JSON keys one to one
 * (same names the query itself selects), so Jackson needs no renaming annotations to bind it.
 */
public record LeetCodeDailyChallenge(String date, String link, Question question) {

    public record Question(String questionFrontendId, String title, String titleSlug, String difficulty) {
    }
}

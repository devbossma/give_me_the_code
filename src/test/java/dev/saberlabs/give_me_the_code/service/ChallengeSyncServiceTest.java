package dev.saberlabs.give_me_the_code.service;

import dev.saberlabs.give_me_the_code.client.LeetCodeClient;
import dev.saberlabs.give_me_the_code.client.LeetCodeDailyChallenge;
import dev.saberlabs.give_me_the_code.model.Challenge;
import dev.saberlabs.give_me_the_code.model.Difficulty;
import dev.saberlabs.give_me_the_code.repository.ChallengeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeSyncServiceTest {

    @Mock
    private ChallengeRepository challengeRepository;
    @Mock
    private LeetCodeClient leetCodeClient;

    private ChallengeSyncService challengeSyncService;

    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

    private final LeetCodeDailyChallenge dailyChallenge = new LeetCodeDailyChallenge(
            today.toString(), "/problems/two-sum/",
            new LeetCodeDailyChallenge.Question("1", "Two Sum", "two-sum", "Easy"));

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        challengeSyncService = new ChallengeSyncService(challengeRepository, leetCodeClient);
    }

    @Test
    void returnsTheCachedRowWithoutCallingLeetCodeOnACacheHit() {
        Challenge cached = new Challenge("1", "Two Sum", "two-sum", Difficulty.EASY,
                "https://leetcode.com/problems/two-sum/", "<p>...</p>", today);
        when(challengeRepository.findByChallengeDate(today)).thenReturn(Optional.of(cached));

        Challenge result = challengeSyncService.getOrFetchToday();

        assertThat(result).isSameAs(cached);
        verify(leetCodeClient, never()).fetchDailyChallenge();
    }

    @Test
    void fetchesFromLeetCodeAndInsertsANewRowOnACacheMiss() {
        when(challengeRepository.findByChallengeDate(today)).thenReturn(Optional.empty());
        when(leetCodeClient.fetchDailyChallenge()).thenReturn(dailyChallenge);
        when(leetCodeClient.fetchQuestionContent("two-sum")).thenReturn("<p>Given an array...</p>");
        when(challengeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Challenge result = challengeSyncService.getOrFetchToday();

        assertThat(result.getLeetcodeQuestionId()).isEqualTo("1");
        assertThat(result.getTitle()).isEqualTo("Two Sum");
        assertThat(result.getTitleSlug()).isEqualTo("two-sum");
        assertThat(result.getDifficulty()).isEqualTo(Difficulty.EASY);
        assertThat(result.getLink()).isEqualTo("https://leetcode.com/problems/two-sum/");
        assertThat(result.getContent()).isEqualTo("<p>Given an array...</p>");
        assertThat(result.getChallengeDate()).isEqualTo(today);
    }

    @Test
    void leavesAnAlreadyAbsoluteLinkUntouched() {
        LeetCodeDailyChallenge absoluteLink = new LeetCodeDailyChallenge(
                today.toString(), "https://leetcode.com/problems/two-sum/",
                new LeetCodeDailyChallenge.Question("1", "Two Sum", "two-sum", "Easy"));
        when(challengeRepository.findByChallengeDate(today)).thenReturn(Optional.empty());
        when(leetCodeClient.fetchDailyChallenge()).thenReturn(absoluteLink);
        when(leetCodeClient.fetchQuestionContent("two-sum")).thenReturn("content");
        when(challengeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Challenge result = challengeSyncService.getOrFetchToday();

        assertThat(result.getLink()).isEqualTo("https://leetcode.com/problems/two-sum/");
    }

    @Test
    void refreshUpdatesAnExistingRowInPlaceInsteadOfInsertingASecondOne() {
        Challenge existing = new Challenge("1", "Two Sum (old title)", "two-sum", Difficulty.MEDIUM,
                "https://leetcode.com/problems/two-sum/", "stale content", today);
        when(challengeRepository.findByChallengeDate(today)).thenReturn(Optional.of(existing));
        when(leetCodeClient.fetchDailyChallenge()).thenReturn(dailyChallenge);
        when(leetCodeClient.fetchQuestionContent("two-sum")).thenReturn("fresh content");
        when(challengeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        challengeSyncService.refreshDailyChallenge();

        ArgumentCaptor<Challenge> savedCaptor = ArgumentCaptor.forClass(Challenge.class);
        verify(challengeRepository).save(savedCaptor.capture());
        assertThat(savedCaptor.getValue()).isSameAs(existing);
        assertThat(savedCaptor.getValue().getTitle()).isEqualTo("Two Sum");
        assertThat(savedCaptor.getValue().getDifficulty()).isEqualTo(Difficulty.EASY);
        assertThat(savedCaptor.getValue().getContent()).isEqualTo("fresh content");
    }

    @Test
    void aScheduledRefreshFailureIsSwallowedRatherThanPropagated() {
        when(leetCodeClient.fetchDailyChallenge()).thenThrow(new RuntimeException("LeetCode is down"));

        assertThatCode(() -> challengeSyncService.refreshDailyChallenge()).doesNotThrowAnyException();
    }
}

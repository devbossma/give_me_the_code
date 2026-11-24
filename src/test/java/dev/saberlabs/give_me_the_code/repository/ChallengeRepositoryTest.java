package dev.saberlabs.give_me_the_code.repository;

import dev.saberlabs.give_me_the_code.SharedMySqlContainer;
import dev.saberlabs.give_me_the_code.model.Challenge;
import dev.saberlabs.give_me_the_code.model.Difficulty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ChallengeRepositoryTest extends SharedMySqlContainer {

    @Autowired
    private ChallengeRepository challengeRepository;

    @Test
    void findsAChallengeByItsDate() {
        LocalDate date = LocalDate.of(2026, 11, 24);
        challengeRepository.save(new Challenge("1", "Two Sum", "two-sum", Difficulty.EASY,
                "https://leetcode.com/problems/two-sum/", "content", date));

        assertThat(challengeRepository.findByChallengeDate(date)).isPresent();
        assertThat(challengeRepository.findByChallengeDate(date.plusDays(1))).isEmpty();
    }

    @Test
    void onlyOneChallengeIsAllowedPerDate() {
        LocalDate date = LocalDate.of(2026, 11, 25);
        challengeRepository.save(new Challenge("1", "Two Sum", "two-sum", Difficulty.EASY,
                "https://leetcode.com/problems/two-sum/", "content", date));

        assertThatThrownBy(() -> challengeRepository.saveAndFlush(
                new Challenge("2", "Add Two Numbers", "add-two-numbers", Difficulty.MEDIUM,
                        "https://leetcode.com/problems/add-two-numbers/", "content", date)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findsTheMostRecentChallenge() {
        LocalDate earlier = LocalDate.of(2026, 11, 20);
        LocalDate later = LocalDate.of(2026, 11, 21);
        challengeRepository.save(new Challenge("1", "Two Sum", "two-sum", Difficulty.EASY,
                "https://leetcode.com/problems/two-sum/", "content", earlier));
        challengeRepository.save(new Challenge("2", "Add Two Numbers", "add-two-numbers", Difficulty.MEDIUM,
                "https://leetcode.com/problems/add-two-numbers/", "content", later));

        assertThat(challengeRepository.findTopByOrderByChallengeDateDesc())
                .get().extracting(Challenge::getChallengeDate).isEqualTo(later);
    }
}

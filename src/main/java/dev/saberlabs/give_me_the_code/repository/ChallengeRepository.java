package dev.saberlabs.give_me_the_code.repository;

import dev.saberlabs.give_me_the_code.model.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    Optional<Challenge> findByChallengeDate(LocalDate challengeDate);

    Optional<Challenge> findTopByOrderByChallengeDateDesc();
}

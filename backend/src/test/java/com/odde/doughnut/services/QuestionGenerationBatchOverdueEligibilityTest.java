package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchOverdueEligibilityTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;

  User user;
  Note note;
  MemoryTracker memoryTracker;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    note = makeMe.aNote().notebookOwnedBy(user).please();
    memoryTracker = makeMe.aMemoryTrackerFor(note).please();
  }

  @ParameterizedTest
  @MethodSource("overdueScenarios")
  void includesUserWhenOverdueAfterTargetTimePassed(LocalDateTime cron, LocalDateTime recall) {
    createAnsweredRecall(Timestamp.valueOf(recall));

    List<User> candidates =
        planningService.findUsersEligibleForBatchSubmission(Timestamp.valueOf(cron));

    assertThat(candidates.stream().map(User::getId).toList(), contains(user.getId()));
  }

  static Stream<Arguments> overdueScenarios() {
    return Stream.of(
        Arguments.of(LocalDateTime.of(2024, 6, 15, 10, 30), LocalDateTime.of(2024, 6, 15, 9, 30)),
        Arguments.of(LocalDateTime.of(2024, 6, 15, 12, 15), LocalDateTime.of(2024, 6, 15, 9, 30)),
        Arguments.of(LocalDateTime.of(2024, 6, 16, 0, 30), LocalDateTime.of(2024, 6, 15, 23, 45)));
  }

  @Test
  void excludesUserWhoSubmittedAfterDueInstant() {
    Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 30));
    createAnsweredRecall(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 0)));
    makeMe
        .aQuestionGenerationBatch()
        .forUser(user)
        .completedAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 0)))
        .please();
    makeMe.entityPersister.flush();

    assertThat(planningService.findUsersEligibleForBatchSubmission(cronTime), empty());
  }

  private void createAnsweredRecall(Timestamp answerTime) {
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(memoryTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(answerTime)
        .please();
  }
}

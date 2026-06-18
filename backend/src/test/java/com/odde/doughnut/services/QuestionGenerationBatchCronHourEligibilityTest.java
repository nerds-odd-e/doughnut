package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatchUserState;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchUserStateRepository;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchCronHourEligibilityTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchUserStateRepository userStateRepository;

  User user;
  Note note;
  MemoryTracker memoryTracker;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    note = makeMe.aNote().notebookOwnedBy(user).please();
    memoryTracker = makeMe.aMemoryTrackerFor(note).by(user).please();
  }

  @Test
  void includesUserWhoseTargetFallsInCurrentCronHour() {
    Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 30));
    Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 30));
    createAnsweredRecall(recallTime);

    List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

    assertThat(candidates.stream().map(User::getId).toList(), contains(user.getId()));
  }

  @Test
  void excludesUserWhoseTargetFallsInAdjacentCronHour() {
    Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 11, 15));
    Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 30));
    createAnsweredRecall(recallTime);

    List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

    assertThat(candidates, empty());
  }

  @Test
  void includesUserWhoseTargetCrossesMidnightWhenCronHourMatches() {
    Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 16, 0, 30));
    Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 23, 45));
    createAnsweredRecall(recallTime);

    List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

    assertThat(candidates.stream().map(User::getId).toList(), contains(user.getId()));
  }

  @Test
  void excludesUserPastSubmissionGateEvenWhenDueInCurrentCronHour() {
    Timestamp cronTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 10, 30));
    Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 30));
    createAnsweredRecall(recallTime);
    QuestionGenerationBatchUserState state = new QuestionGenerationBatchUserState();
    state.setUser(user);
    state.setLastSuccessfulSubmittedAt(
        new Timestamp(cronTime.getTime() - TimeUnit.HOURS.toMillis(1)));
    userStateRepository.save(state);
    makeMe.entityPersister.flush();

    List<User> candidates = planningService.findUsersEligibleForBatchSubmission(cronTime);

    assertThat(candidates, empty());
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

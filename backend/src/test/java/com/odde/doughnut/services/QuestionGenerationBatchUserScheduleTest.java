package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.odde.doughnut.controllers.dto.QuestionGenerationBatchUserScheduleDTO;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
class QuestionGenerationBatchUserScheduleTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchRepository batchRepository;

  User user;
  Timestamp currentTime;
  Note note;
  MemoryTracker dueTracker;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().of(10, 8).fromShanghai().please();
    note = makeMe.aNote().notebookOwnedBy(user).please();
    dueTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .by(user)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
            .please();
  }

  @Test
  void schedulesCurrentTopOfHourWhenUserIsDueAndHasNoPreviousSuccessfulSubmission() {
    Timestamp now = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 9, 0));
    givenAnsweredRecallAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 8, 30)));

    QuestionGenerationBatchUserScheduleDTO schedule =
        planningService.getNextBatchQuestionSchedule(user, now);

    assertThat(schedule.getNextScheduledAt(), is(now));
    assertThat(schedule.getReason(), is((String) null));
  }

  @Test
  void catchesUpSameDayWhenOverdueAfterMissedTargetHour() {
    Timestamp now = Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 11, 0));
    givenAnsweredRecallAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 8, 30)));
    givenPriorSubmittedBatchAt(Timestamp.valueOf(LocalDateTime.of(2024, 6, 14, 11, 30)));

    QuestionGenerationBatchUserScheduleDTO schedule =
        planningService.getNextBatchQuestionSchedule(user, now);

    assertThat(
        schedule.getNextScheduledAt(), is(Timestamp.valueOf(LocalDateTime.of(2024, 6, 15, 11, 0))));
  }

  @Test
  void returnsInProgressReasonWhenSubmittedBatchExists() {
    QuestionGenerationBatch batch = new QuestionGenerationBatch();
    batch.setUser(user);
    batch.setStatus(QuestionGenerationBatchStatus.SUBMITTED);
    batch.setPlannedAt(currentTime);
    batchRepository.save(batch);

    QuestionGenerationBatchUserScheduleDTO schedule =
        planningService.getNextBatchQuestionSchedule(user, currentTime);

    assertThat(schedule.getNextScheduledAt(), is((Timestamp) null));
    assertThat(
        schedule.getReason(), is(QuestionGenerationBatchPlanningService.REASON_BATCH_IN_PROGRESS));
  }

  @Test
  void returnsNoRecentRecallsReasonWhenUserHasNoRecentAnsweredRecall() {
    QuestionGenerationBatchUserScheduleDTO schedule =
        planningService.getNextBatchQuestionSchedule(user, currentTime);

    assertThat(schedule.getNextScheduledAt(), is((Timestamp) null));
    assertThat(
        schedule.getReason(), is(QuestionGenerationBatchPlanningService.REASON_NO_RECENT_RECALLS));
  }

  private void givenAnsweredRecallAt(Timestamp answeredAt) {
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(dueTracker)
        .answerChoiceIndex(0)
        .answerTimestamp(answeredAt)
        .please();
  }

  private void givenPriorSubmittedBatchAt(Timestamp submittedAt) {
    QuestionGenerationBatch batch = new QuestionGenerationBatch();
    batch.setUser(user);
    batch.setStatus(QuestionGenerationBatchStatus.COMPLETED);
    batch.setPlannedAt(submittedAt);
    batch.setSubmittedAt(submittedAt);
    batchRepository.save(batch);
    makeMe.entityPersister.flush();
  }
}

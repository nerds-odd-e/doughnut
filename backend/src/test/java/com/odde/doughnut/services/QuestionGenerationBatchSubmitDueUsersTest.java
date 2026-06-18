package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.dto.QuestionGenerationBatchSubmissionSummaryDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchUserState;
import com.odde.doughnut.entities.User;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

@Isolated
class QuestionGenerationBatchSubmitDueUsersTest
    extends QuestionGenerationBatchSubmitDueUsersTestBase {

  @Test
  void continuesAfterPerUserFailureAndUpdatesSuccessfulUserMarker() {
    User[] users = new User[2];
    inCommittedTransaction(
        () -> {
          users[0] = uniqueUser();
          users[1] = uniqueUser();
          Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 8, 3, 15, 45));

          Note successfulNote =
              makeMe
                  .aNote()
                  .notebook(makeMe.aNotebook().creatorAndOwner(users[0]).please())
                  .please();
          var successfulTracker =
              makeMe
                  .aMemoryTrackerFor(successfulNote)
                  .by(users[0])
                  .nextRecallAt(new Timestamp(cronTime.getTime() + TimeUnit.HOURS.toMillis(24)))
                  .please();
          makeMe
              .aRecallPrompt()
              .withPredefinedQuestionForNote(successfulNote)
              .forMemoryTracker(successfulTracker)
              .answerChoiceIndex(0)
              .answerTimestamp(recallTime)
              .please();

          Note failingNote =
              makeMe
                  .aNote()
                  .notebook(makeMe.aNotebook().creatorAndOwner(users[1]).please())
                  .please();
          var failingTracker =
              makeMe
                  .aMemoryTrackerFor(failingNote)
                  .by(users[1])
                  .nextRecallAt(new Timestamp(cronTime.getTime() + TimeUnit.HOURS.toMillis(24)))
                  .please();
          makeMe
              .aRecallPrompt()
              .withPredefinedQuestionForNote(failingNote)
              .forMemoryTracker(failingTracker)
              .answerChoiceIndex(0)
              .answerTimestamp(recallTime)
              .please();
        });

    doReturn(List.of(users[0], users[1]))
        .when(planningService)
        .findUsersEligibleForBatchSubmission(cronTime);

    when(openAiApiHandler.uploadBatchInputFile(any())).thenReturn("file-ok");
    when(openAiApiHandler.createResponsesBatch(any()))
        .thenReturn("batch-ok")
        .thenThrow(new RuntimeException("batch create failed"));

    QuestionGenerationBatchSubmissionSummaryDTO[] summary =
        new QuestionGenerationBatchSubmissionSummaryDTO[1];
    inCommittedTransaction(() -> summary[0] = submitDueUsersService.submitDueUsers(cronTime));

    inCommittedTransaction(
        () -> {
          assertThat(summary[0].getConsideredUserCount(), equalTo(2));
          assertThat(summary[0].getSubmittedCount(), equalTo(1));
          assertThat(summary[0].getFailedCount(), equalTo(1));
          assertThat(summary[0].getSkippedCount(), equalTo(0));

          List<QuestionGenerationBatch> testBatches =
              batchRepository.findAll().stream()
                  .filter(
                      batch ->
                          batch.getUser().getId().equals(users[0].getId())
                              || batch.getUser().getId().equals(users[1].getId()))
                  .toList();

          QuestionGenerationBatch submittedBatch =
              testBatches.stream()
                  .filter(batch -> batch.getStatus() == QuestionGenerationBatchStatus.SUBMITTED)
                  .findFirst()
                  .orElseThrow();
          QuestionGenerationBatchUserState successfulState =
              userStateRepository.findByUser_Id(submittedBatch.getUser().getId()).orElseThrow();
          assertThat(successfulState.getLastSuccessfulSubmittedAt(), equalTo(cronTime));
          assertThat(submittedBatch.getOpenaiBatchId(), equalTo("batch-ok"));

          QuestionGenerationBatch failedBatch =
              testBatches.stream()
                  .filter(batch -> batch.getStatus() == QuestionGenerationBatchStatus.FAILED)
                  .findFirst()
                  .orElseThrow();
          assertThat(
              userStateRepository.findByUser_Id(failedBatch.getUser().getId()).isPresent(),
              is(false));
        });
  }

  @Test
  void skipsDueUserWithNoCandidateTrackersWithoutUpdatingMarker() {
    User[] dueUser = new User[1];
    inCommittedTransaction(
        () -> {
          dueUser[0] = uniqueUser();

          Note note =
              makeMe
                  .aNote()
                  .notebook(makeMe.aNotebook().creatorAndOwner(dueUser[0]).please())
                  .please();
          var tracker =
              makeMe
                  .aMemoryTrackerFor(note)
                  .by(dueUser[0])
                  .nextRecallAt(new Timestamp(cronTime.getTime() + TimeUnit.HOURS.toMillis(24)))
                  .please();
          Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 8, 3, 15, 45));
          makeMe
              .aRecallPrompt()
              .withPredefinedQuestionForNote(note)
              .forMemoryTracker(tracker)
              .answerChoiceIndex(0)
              .answerTimestamp(recallTime)
              .please();
          makeMe
              .aRecallPrompt()
              .withPredefinedQuestionForNote(note)
              .forMemoryTracker(tracker)
              .please();
        });

    doReturn(List.of(dueUser[0]))
        .when(planningService)
        .findUsersEligibleForBatchSubmission(cronTime);

    QuestionGenerationBatchSubmissionSummaryDTO[] summary =
        new QuestionGenerationBatchSubmissionSummaryDTO[1];
    inCommittedTransaction(() -> summary[0] = submitDueUsersService.submitDueUsers(cronTime));

    inCommittedTransaction(
        () -> {
          assertThat(summary[0].getConsideredUserCount(), equalTo(1));
          assertThat(summary[0].getSubmittedCount(), equalTo(0));
          assertThat(summary[0].getFailedCount(), equalTo(0));
          assertThat(summary[0].getSkippedCount(), equalTo(1));

          assertThat(
              batchRepository.findAll().stream()
                  .anyMatch(batch -> batch.getUser().getId().equals(dueUser[0].getId())),
              is(false));
          assertThat(userStateRepository.findByUser_Id(dueUser[0].getId()).isPresent(), is(false));
        });
  }
}

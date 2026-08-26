package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;

import com.odde.donut.controllers.dto.QuestionGenerationBatchSubmissionSummaryDTO;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.User;
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
  void skipsDueUserWithNoCandidateTrackersWithoutSubmittedBatch() {
    User[] dueUser = new User[1];
    inCommittedTransaction(
        () -> {
          dueUser[0] = uniqueUser();

          Note note = makeMe.aNote().notebookOwnedBy(dueUser[0]).please();
          var tracker =
              makeMe
                  .aMemoryTrackerFor(note)
                  .nextRecallAt(new Timestamp(cronTime.getTime() + TimeUnit.HOURS.toMillis(24)))
                  .please();
          Timestamp recallTime = Timestamp.valueOf(LocalDateTime.of(2024, 8, 3, 15, 45));
          makeMe
              .aRecallPrompt()
              .withMcqForNote(note)
              .forMemoryTracker(tracker)
              .answerChoiceIndex(0)
              .answerTimestamp(recallTime)
              .please();
          makeMe.aRecallPrompt().withMcqForNote(note).forMemoryTracker(tracker).please();
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
          assertThat(
              batchRepository.findLatestSubmittedAtByUser_Id(dueUser[0].getId()).isPresent(),
              is(false));
        });
  }
}

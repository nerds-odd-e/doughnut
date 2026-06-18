package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
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
class QuestionGenerationBatchLocalPlanningTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;

  User user;
  Timestamp currentTime;
  Note note;
  MemoryTracker dueTracker;
  MemoryTracker secondDueTracker;

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
    secondDueTracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().notebookOwnedBy(user).please())
            .by(user)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(12)))
            .please();
  }

  @Test
  void createsPlannedBatchWithOneRequestPerCandidateTracker() {
    Optional<QuestionGenerationBatch> plannedBatch =
        planningService.planLocalBatchForUser(user, currentTime);

    assertThat(plannedBatch.isPresent(), is(true));
    QuestionGenerationBatch batch = plannedBatch.get();
    assertThat(batch.getStatus(), is(QuestionGenerationBatchStatus.PLANNED));
    assertThat(batch.getUser().getId(), is(user.getId()));
    assertThat(batch.getPlannedAt(), is(currentTime));

    List<QuestionGenerationBatchRequest> requests =
        batchRequestRepository.findByBatch_Id(batch.getId());
    assertThat(requests, hasSize(2));
    assertThat(
        requests.stream().map(request -> request.getMemoryTracker().getId()).toList(),
        containsInAnyOrder(dueTracker.getId(), secondDueTracker.getId()));

    for (QuestionGenerationBatchRequest request : requests) {
      assertThat(request.getContextSeed(), is(notNullValue()));
      assertThat(
          request.getCustomId(),
          is(
              QuestionGenerationBatchRequest.customIdFor(
                  batch.getId(), request.getMemoryTracker().getId())));
    }
  }

  @Test
  void createsNoBatchWhenUserHasNoCandidateTrackers() {
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(note)
        .forMemoryTracker(dueTracker)
        .please();
    makeMe
        .aRecallPrompt()
        .withPredefinedQuestionForNote(secondDueTracker.getNote())
        .forMemoryTracker(secondDueTracker)
        .please();

    Optional<QuestionGenerationBatch> plannedBatch =
        planningService.planLocalBatchForUser(user, currentTime);

    assertThat(plannedBatch, is(Optional.empty()));
    assertThat(batchRepository.count(), is(0L));
    assertThat(batchRequestRepository.count(), is(0L));
  }
}

package com.odde.doughnut.services;

import static com.odde.doughnut.controllers.dto.Randomization.RandomStrategy.first;
import static com.odde.doughnut.controllers.dto.Randomization.RandomStrategy.last;
import static com.odde.doughnut.services.QuestionGenerationBatchImportPayloadSupport.batchSuccessLine;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.Randomization;
import com.odde.doughnut.entities.Mcq;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.QuestionType;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.services.ai.GeneratedMcq;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.TestabilitySettings;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchRowImportServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchRowImportService rowImportService;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired RecallPromptRepository recallPromptRepository;
  @Autowired TestabilitySettings testabilitySettings;

  User user;
  Timestamp currentTime;
  MemoryTracker memoryTracker;
  QuestionGenerationBatchRequest outputReadyRequest;
  GeneratedMcq generatedMcq;

  @BeforeEach
  void setup() throws JsonProcessingException {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().please();

    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    memoryTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
            .please();

    QuestionGenerationBatch batch =
        makeMe
            .aQuestionGenerationBatch()
            .forUser(user)
            .status(QuestionGenerationBatchStatus.COMPLETED)
            .plannedAt(currentTime)
            .please();
    makeMe.entityPersister.flush();

    generatedMcq =
        makeMe
            .aGeneratedMcq()
            .stem("What color is the sky on a clear day?")
            .choices("Blue", "Green", "Red")
            .correctAnswerIndex(0)
            .please();

    outputReadyRequest =
        makeMe
            .aQuestionGenerationBatchRequest()
            .batch(batch)
            .memoryTracker(memoryTracker)
            .status(QuestionGenerationBatchRequestStatus.OUTPUT_READY)
            .please();
    outputReadyRequest.setRawSuccessPayload(
        batchSuccessLine(outputReadyRequest.getCustomId(), generatedMcq));
    batchRequestRepository.saveAndFlush(outputReadyRequest);
  }

  @AfterEach
  void cleanup() {
    testabilitySettings.setRandomization(new Randomization(first, 0));
  }

  @Nested
  class ImportOutputReadyRow {
    @Test
    void createsPostProcessedMcqAndRecallPromptFromBatchOutput() {
      testabilitySettings.setRandomization(new Randomization(last, 0));

      assertThat(rowImportService.importRow(outputReadyRequest), is(true));

      QuestionGenerationBatchRequest reloadedRequest =
          batchRequestRepository.findById(outputReadyRequest.getId()).orElseThrow();
      assertThat(reloadedRequest.getStatus(), is(QuestionGenerationBatchRequestStatus.IMPORTED));

      List<RecallPrompt> recallPrompts =
          recallPromptRepository.findAllByMemoryTracker_IdOrderByIdDesc(memoryTracker.getId());
      assertThat(recallPrompts.size(), is(1));

      RecallPrompt recallPrompt = recallPrompts.get(0);
      assertThat(recallPrompt.getQuestionType(), is(QuestionType.MCQ));
      assertThat(recallPrompt.getMemoryTracker().getId(), is(memoryTracker.getId()));

      Mcq mcq = recallPrompt.getMcq();
      assertThat(mcq.getNote().getId(), is(memoryTracker.getNote().getId()));
      assertThat(mcq.getContextSeed(), is(outputReadyRequest.getContextSeed()));
      assertThat(mcq.isContested(), is(false));

      assertThat(mcq.getQuestionStem(), is(generatedMcq.getQuestionStem()));
      assertThat(mcq.getResponseChoices(), is(List.of("Red", "Green", "Blue")));
      assertThat(mcq.getCorrectAnswerIndex(), is(2));
    }

    @Test
    void reimportingSameRowDoesNotCreateDuplicates() {
      assertThat(rowImportService.importRow(outputReadyRequest), is(true));
      assertThat(rowImportService.importRow(outputReadyRequest), is(false));

      assertThat(
          recallPromptRepository
              .findAllByMemoryTracker_IdOrderByIdDesc(memoryTracker.getId())
              .size(),
          is(1));
    }
  }
}

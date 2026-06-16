package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.QuestionType;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.PredefinedQuestionRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.MakeMe;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapperConfig().objectMapper();

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchRowImportService rowImportService;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired PredefinedQuestionRepository predefinedQuestionRepository;
  @Autowired RecallPromptRepository recallPromptRepository;

  User user;
  Timestamp currentTime;
  MemoryTracker memoryTracker;
  QuestionGenerationBatchRequest outputReadyRequest;
  MCQWithAnswer mcqWithAnswer;

  @BeforeEach
  void setup() throws JsonProcessingException {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().please();

    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    memoryTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .by(user)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
            .please();

    QuestionGenerationBatch batch = new QuestionGenerationBatch();
    batch.setUser(user);
    batch.setStatus(QuestionGenerationBatchStatus.COMPLETED);
    batch.setPlannedAt(currentTime);
    batch = batchRepository.saveAndFlush(batch);

    outputReadyRequest = new QuestionGenerationBatchRequest();
    outputReadyRequest.setBatch(batch);
    outputReadyRequest.setMemoryTracker(memoryTracker);
    outputReadyRequest.setContextSeed(42L);
    outputReadyRequest.setCustomId(
        QuestionGenerationBatchRequest.customIdFor(batch.getId(), memoryTracker.getId()));

    mcqWithAnswer =
        makeMe
            .aMCQWithAnswer()
            .stem("What color is the sky on a clear day?")
            .choices("Blue", "Green", "Red")
            .correctChoiceIndex(0)
            .please();
    outputReadyRequest.setStatus(QuestionGenerationBatchRequestStatus.OUTPUT_READY);
    outputReadyRequest.setRawSuccessPayload(
        batchSuccessLine(outputReadyRequest.getCustomId(), mcqWithAnswer));
    batchRequestRepository.saveAndFlush(outputReadyRequest);
  }

  @Nested
  class ImportOutputReadyRow {
    @Test
    void createsPredefinedQuestionAndRecallPromptFromBatchOutput() {
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

      PredefinedQuestion predefinedQuestion = recallPrompt.getPredefinedQuestion();
      assertThat(predefinedQuestion.getNote().getId(), is(memoryTracker.getNote().getId()));
      assertThat(predefinedQuestion.getContextSeed(), is(outputReadyRequest.getContextSeed()));
      assertThat(predefinedQuestion.isContested(), is(false));

      MCQWithAnswer importedMcq = predefinedQuestion.getMcqWithAnswer();
      assertThat(
          importedMcq.getQuestion().getQuestionStem(),
          is(mcqWithAnswer.getQuestion().getQuestionStem()));
      assertThat(
          importedMcq.getQuestion().getResponseChoices(),
          is(mcqWithAnswer.getQuestion().getResponseChoices()));
      assertThat(importedMcq.getSolutionChoiceIndex(), is(mcqWithAnswer.getSolutionChoiceIndex()));
    }

    @Test
    void reimportingSameRowDoesNotCreateDuplicates() {
      assertThat(rowImportService.importRow(outputReadyRequest), is(true));
      assertThat(rowImportService.importRow(outputReadyRequest), is(false));

      List<RecallPrompt> recallPrompts =
          recallPromptRepository.findAllByMemoryTracker_IdOrderByIdDesc(memoryTracker.getId());
      assertThat(recallPrompts.size(), is(1));

      long predefinedQuestionCount =
          ((List<PredefinedQuestion>) predefinedQuestionRepository.findAll()).size();
      assertThat(predefinedQuestionCount, is(1L));
    }
  }

  private static String batchSuccessLine(String customId, MCQWithAnswer mcqWithAnswer)
      throws JsonProcessingException {
    String structuredOutput = OBJECT_MAPPER.writeValueAsString(mcqWithAnswer);
    String responseBody =
        """
        {
          "id": "resp-1",
          "status": "completed",
          "output": [
            {
              "type": "message",
              "id": "msg-1",
              "status": "completed",
              "content": [
                {
                  "type": "output_text",
                  "text": %s
                }
              ]
            }
          ]
        }
        """
            .formatted(OBJECT_MAPPER.writeValueAsString(structuredOutput));

    return """
        {"id":"batch_req_1","custom_id":"%s","response":{"status_code":200,"body":%s},"error":null}"""
        .formatted(customId, responseBody);
  }
}

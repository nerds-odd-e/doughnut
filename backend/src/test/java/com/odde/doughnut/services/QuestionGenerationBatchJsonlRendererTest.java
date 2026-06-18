package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.openAiApis.StructuredResponseCreateParamsSerializer;
import com.odde.doughnut.testability.MakeMe;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
class QuestionGenerationBatchJsonlRendererTest {

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchJsonlRenderer jsonlRenderer;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationRequestBuilder requestBuilder;
  @Autowired GlobalSettingsService globalSettingsService;
  @Autowired StructuredResponseCreateParamsSerializer paramsSerializer;

  private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

  private User user;
  private Timestamp currentTime;

  @BeforeEach
  void setup() {
    user = makeMe.aUser().please();
    currentTime = makeMe.aTimestamp().please();
    globalSettingsService
        .globalSettingQuestionGeneration()
        .setKeyValue(currentTime, "gpt-batch-question-generation");
  }

  private List<Map<String, Object>> parseJsonl(String jsonl) throws Exception {
    if (jsonl.isBlank()) {
      return List.of();
    }
    List<Map<String, Object>> lines = new ArrayList<>();
    for (String line : jsonl.split("\n", -1)) {
      if (line.isBlank()) {
        continue;
      }
      lines.add(objectMapper.readValue(line, new TypeReference<>() {}));
    }
    return lines;
  }

  private Map<String, Object> expectedBodyForRequest(
      QuestionGenerationBatch batch, QuestionGenerationBatchRequest request) {
    MemoryTracker tracker = request.getMemoryTracker();
    String propertyKey = tracker.getPropertyKey();
    if (propertyKey != null && propertyKey.isBlank()) {
      propertyKey = null;
    }
    StructuredResponseCreateParams<MCQWithAnswer> params =
        requestBuilder.buildQuestionGenerationResponseRequestForBatch(
            tracker.getNote(), null, request.getContextSeed(), propertyKey, batch.getUser());
    return paramsSerializer.toBodyMap(params);
  }

  @Nested
  class RenderInputJsonl {
    @Test
    void rendersOneJsonlLinePerRequestWithResponsesApiShape() throws Exception {
      Note note = makeMe.aNote().notebookOwnedBy(user).please();
      Note propertyNote = makeMe.aNote().notebookOwnedBy(user).please();
      makeMe
          .aMemoryTrackerFor(note)
          .by(user)
          .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
          .please();
      makeMe
          .aMemoryTrackerFor(propertyNote)
          .by(user)
          .propertyKey("topic")
          .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(12)))
          .please();

      QuestionGenerationBatch batch =
          planningService.planLocalBatchForUser(user, currentTime).orElseThrow();
      List<QuestionGenerationBatchRequest> requests =
          batchRequestRepository.findByBatch_Id(batch.getId());

      String jsonl = jsonlRenderer.renderInputJsonl(batch);
      List<Map<String, Object>> lines = parseJsonl(jsonl);

      assertThat(lines, hasSize(requests.size()));
      for (QuestionGenerationBatchRequest request : requests) {
        Optional<Map<String, Object>> matchingLine =
            lines.stream()
                .filter(line -> request.getCustomId().equals(line.get("custom_id")))
                .findFirst();
        assertThat(
            "missing JSONL line for custom id " + request.getCustomId(),
            matchingLine.isPresent(),
            is(true));

        Map<String, Object> line = matchingLine.get();
        assertThat(
            line.get("method"), is(QuestionGenerationBatchJsonlRenderer.BATCH_REQUEST_METHOD));
        assertThat(
            line.get("url"), is(QuestionGenerationBatchJsonlRenderer.BATCH_RESPONSES_ENDPOINT));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) line.get("body");
        Map<String, Object> expectedBody = expectedBodyForRequest(batch, request);
        assertThat(body, is(expectedBody));
        assertThat(body.get("model"), is("gpt-batch-question-generation"));
        assertThat(body.get("max_output_tokens"), is(1000));
        assertThat(body.containsKey("reasoning"), is(false));
        assertThat(body.containsKey("text"), is(true));
        @SuppressWarnings("unchecked")
        Map<String, Object> text = (Map<String, Object>) body.get("text");
        assertThat(text.containsKey("format"), is(true));
      }
    }

    @Test
    void returnsEmptyStringWhenBatchHasNoRequests() throws Exception {
      QuestionGenerationBatch batch = new QuestionGenerationBatch();
      batch.setUser(user);
      batch.setStatus(QuestionGenerationBatchStatus.PLANNED);
      batch.setPlannedAt(currentTime);
      batch = batchRepository.saveAndFlush(batch);

      String jsonl = jsonlRenderer.renderInputJsonl(batch);

      assertThat(jsonl, is(""));
      assertThat(parseJsonl(jsonl), empty());
    }

    @Test
    void includesHighReasoningEffortForReasoningModel() throws Exception {
      globalSettingsService.globalSettingQuestionGeneration().setKeyValue(currentTime, "o3-mini");
      Note note = makeMe.aNote().notebookOwnedBy(user).please();
      makeMe
          .aMemoryTrackerFor(note)
          .by(user)
          .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
          .please();

      QuestionGenerationBatch batch =
          planningService.planLocalBatchForUser(user, currentTime).orElseThrow();
      List<QuestionGenerationBatchRequest> requests =
          batchRequestRepository.findByBatch_Id(batch.getId());
      String jsonl = jsonlRenderer.renderInputJsonl(batch);
      List<Map<String, Object>> lines = parseJsonl(jsonl);

      assertThat(lines, hasSize(1));
      @SuppressWarnings("unchecked")
      Map<String, Object> body = (Map<String, Object>) lines.get(0).get("body");
      assertThat(body, is(expectedBodyForRequest(batch, requests.get(0))));
      assertThat(body.get("max_output_tokens"), is(12000));
      @SuppressWarnings("unchecked")
      Map<String, Object> reasoning = (Map<String, Object>) body.get("reasoning");
      assertThat(reasoning.get("effort"), is("high"));
    }
  }
}

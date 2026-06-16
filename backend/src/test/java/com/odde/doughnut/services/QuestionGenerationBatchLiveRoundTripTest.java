package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuestionGenerationBatch;
import com.odde.doughnut.entities.QuestionGenerationBatchRequest;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.doughnut.entities.repositories.RecallPromptRepository;
import com.odde.doughnut.testability.MakeMe;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manual live verification against OpenAI Batch + Responses API.
 *
 * <p>Run only when explicitly opted in:
 *
 * <pre>
 * OPENAI_BATCH_LIVE_VERIFY=true OPENAI_API_TOKEN=... \
 *   CURSOR_DEV=true nix develop -c backend/gradlew -p backend test \
 *   --tests QuestionGenerationBatchLiveRoundTripTest \
 *   -Dspring.profiles.active=test
 * </pre>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuestionGenerationBatchLiveRoundTripTest {

  private static final String LIVE_VERIFY_FLAG = "OPENAI_BATCH_LIVE_VERIFY";

  /** Supports structured Responses output and reasoning effort used by question generation. */
  private static final String BATCH_COMPATIBLE_MODEL = GlobalSettingsService.DEFAULT_CHAT_MODEL;

  private static final int POLL_INTERVAL_SECONDS = 30;
  private static final int MAX_POLL_ATTEMPTS = 60;

  @DynamicPropertySource
  static void liveOpenAiToken(DynamicPropertyRegistry registry) {
    String token = System.getenv("OPENAI_API_TOKEN");
    if (token != null && !token.isBlank()) {
      registry.add("spring.openai.token", () -> token);
    }
  }

  @Autowired MakeMe makeMe;
  @Autowired QuestionGenerationBatchPlanningService planningService;
  @Autowired QuestionGenerationBatchSubmissionService submissionService;
  @Autowired QuestionGenerationBatchMaintenanceService maintenanceService;
  @Autowired QuestionGenerationBatchJsonlRenderer jsonlRenderer;
  @Autowired QuestionGenerationBatchRepository batchRepository;
  @Autowired QuestionGenerationBatchRequestRepository batchRequestRepository;
  @Autowired RecallPromptRepository recallPromptRepository;
  @Autowired GlobalSettingsService globalSettingsService;

  User user;
  Timestamp currentTime;
  MemoryTracker memoryTracker;

  @BeforeEach
  void assumeLiveVerifyEnabled() {
    assumeTrue(
        "true".equalsIgnoreCase(System.getenv(LIVE_VERIFY_FLAG)),
        () -> "Set " + LIVE_VERIFY_FLAG + "=true to run live OpenAI batch verification");
    String token = System.getenv("OPENAI_API_TOKEN");
    assumeTrue(token != null && !token.isBlank(), "OPENAI_API_TOKEN must be set");

    currentTime = makeMe.aTimestamp().please();
    user = makeMe.aUser().please();
    globalSettingsService
        .globalSettingQuestionGeneration()
        .setKeyValue(currentTime, BATCH_COMPATIBLE_MODEL);

    Note note =
        makeMe
            .aNote()
            .notebookOwnedBy(user)
            .title("Photosynthesis")
            .content("Plants convert sunlight into chemical energy through photosynthesis.")
            .please();
    memoryTracker =
        makeMe
            .aMemoryTrackerFor(note)
            .by(user)
            .nextRecallAt(new Timestamp(currentTime.getTime() + TimeUnit.HOURS.toMillis(24)))
            .please();
  }

  @Test
  void submitsRealBatchAndImportsRecallPrompt() throws Exception {
    QuestionGenerationBatch plannedBatch =
        planningService.planLocalBatchForUser(user, currentTime).orElseThrow();
    assertThat(plannedBatch.getStatus(), is(QuestionGenerationBatchStatus.PLANNED));

    String jsonl = jsonlRenderer.renderInputJsonl(plannedBatch);
    assertThat(jsonl.isBlank(), is(false));

    boolean submitted = submissionService.submitPlannedBatch(plannedBatch, currentTime);
    assertThat(
        "OpenAI batch submission failed — check model/reasoning compatibility",
        submitted,
        is(true));

    QuestionGenerationBatch submittedBatch =
        batchRepository.findById(plannedBatch.getId()).orElseThrow();
    assertThat(submittedBatch.getStatus(), is(QuestionGenerationBatchStatus.SUBMITTED));
    assertThat(submittedBatch.getOpenaiBatchId(), is(notNullValue()));

    QuestionGenerationBatchRequest request =
        batchRequestRepository.findByBatch_Id(submittedBatch.getId()).getFirst();
    assertThat(request.getMemoryTracker().getId(), is(memoryTracker.getId()));

    Timestamp pollTime = currentTime;
    QuestionGenerationBatchStatus terminalStatus =
        waitForTerminalBatch(submittedBatch.getId(), pollTime);

    assertThat(
        "Batch ended in unexpected status: " + terminalStatus,
        terminalStatus,
        is(QuestionGenerationBatchStatus.COMPLETED));

    maintenanceService.resumeExistingBatches(pollTime);

    QuestionGenerationBatch importedBatch =
        batchRepository.findById(submittedBatch.getId()).orElseThrow();
    assertThat(importedBatch.getImportedAt(), is(notNullValue()));

    QuestionGenerationBatchRequest importedRequest =
        batchRequestRepository.findById(request.getId()).orElseThrow();
    if (importedRequest.getStatus() != QuestionGenerationBatchRequestStatus.IMPORTED) {
      String diagnosticPayload =
          importedRequest.getRawSuccessPayload() != null
              ? importedRequest.getRawSuccessPayload()
              : importedRequest.getRawErrorPayload();
      if (diagnosticPayload != null) {
        saveSuccessPayloadFixture(diagnosticPayload);
      }
    }
    assertThat(
        "request status="
            + importedRequest.getStatus()
            + " error="
            + importedRequest.getErrorDetail()
            + " (see openai-batch-fixtures/live_batch_success_line.json)",
        importedRequest.getStatus(),
        is(QuestionGenerationBatchRequestStatus.IMPORTED));
    assertThat(importedRequest.getRawSuccessPayload(), is(notNullValue()));

    saveSuccessPayloadFixture(importedRequest.getRawSuccessPayload());

    List<RecallPrompt> recallPrompts =
        recallPromptRepository.findAllByMemoryTracker_IdOrderByIdDesc(memoryTracker.getId());
    assertThat(recallPrompts.size(), is(greaterThan(0)));
    assertThat(
        recallPrompts
            .getFirst()
            .getPredefinedQuestion()
            .getMultipleChoicesQuestion()
            .getQuestionStem(),
        is(notNullValue()));
    assertThat(
        recallPrompts
            .getFirst()
            .getPredefinedQuestion()
            .getMultipleChoicesQuestion()
            .getQuestionStem(),
        is(not("")));
  }

  private QuestionGenerationBatchStatus waitForTerminalBatch(Integer batchId, Timestamp pollTime)
      throws InterruptedException {
    for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
      maintenanceService.resumeExistingBatches(pollTime);

      QuestionGenerationBatch batch = batchRepository.findById(batchId).orElseThrow();
      if (batch.getStatus().isTerminal()) {
        return batch.getStatus();
      }

      Thread.sleep(TimeUnit.SECONDS.toMillis(POLL_INTERVAL_SECONDS));
      pollTime = new Timestamp(System.currentTimeMillis());
    }
    return batchRepository.findById(batchId).orElseThrow().getStatus();
  }

  private void saveSuccessPayloadFixture(String rawSuccessPayload) throws Exception {
    Path fixtureDir =
        Path.of("src/test/resources/openai-batch-fixtures").toAbsolutePath().normalize();
    Files.createDirectories(fixtureDir);
    Path fixtureFile = fixtureDir.resolve("live_batch_success_line.json");
    Files.writeString(fixtureFile, rawSuccessPayload);
  }
}

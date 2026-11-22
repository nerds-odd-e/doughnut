package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.JsonNode;
import com.odde.donut.entities.FocusNoteInput;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.donut.services.FocusNoteFingerprint;
import com.odde.donut.services.QuestionGenerationBatchPlanningService;
import com.odde.donut.services.QuestionGenerationBatchRowImportService;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MemoryTrackerBatchRecallPromptControllerTest
    extends MemoryTrackerBatchImportControllerTestBase {
  @Autowired QuestionGenerationBatchRowImportService rowImporter;
  @Autowired QuestionGenerationBatchPlanningService planner;
  @Autowired QuestionGenerationBatchRequestRepository requests;
  @Autowired NotebookGitBindingRepository notebookGitBindings;

  @Test
  void reusesTheImportedQuestionWhenItsRenderedFocusStillMatches() throws Exception {
    RecallPrompt imported = importProviderQuestion(renderRequestMetadata());
    var offered = controller.getRecallPrompt(reloadTracker());

    assertThat(offered.getId(), is(imported.getId()));
    assertThat(offered.getMcq().getQuestionStem(), is(BATCH_STEM));
    assertThat(offered.getMcq().getResponseChoices().getFirst(), is("Nine"));
    verifyNoInteractions(synchronousApi.responseService());
  }

  @Test
  void recordsTheRenderedFocusInputWithTheImportedQuestion() throws Exception {
    Timestamp readAt = testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(2, 3).please());
    JsonNode requestMetadata = renderRequestMetadata();
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(3, 3).please());

    importProviderQuestion(requestMetadata);
    Note focus = reloadTracker().getNote();

    assertThat(
        savedPrompts().getFirst().getMcq().getFocusNoteInput(),
        equalTo(new FocusNoteInput(focus.getId(), readAt, FocusNoteFingerprint.of(focus))));
  }

  @Test
  void replacesTheImportedQuestionWhenAnEditLandsAfterRendering() throws Exception {
    JsonNode requestMetadata = renderRequestMetadata();
    String originalRevision = acceptedRevision();
    publishCorrectedFocus();
    assertThat(acceptedRevision(), not(equalTo(originalRevision)));

    RecallPrompt imported = importProviderQuestion(requestMetadata);
    stubCurrentQuestion("Ten");
    var offered = controller.getRecallPrompt(reloadTracker());

    assertThat(offered.getId(), not(equalTo(imported.getId())));
    assertThat(offered.getMcq().getResponseChoices().getFirst(), is("Ten"));
    assertThat(synchronousApi.requestInputTexts(1).getFirst(), containsString(CORRECTED));
    assertThat(
        savedPrompts().stream().map(RecallPrompt::getId).toList(), contains(offered.getId()));
  }

  @Test
  void plansABatchReplacementFromTheEditedFocusBeforeRecall() throws Exception {
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(2, 3).please());
    importProviderQuestion(renderRequestMetadata());
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(3, 3).please());
    publishCorrectedFocus();
    makeMe.entityPersister.flushAndClear();

    QuestionGenerationBatch replacement =
        planner
            .planLocalBatchForUser(currentUser.getUser(), reloadTracker().getNextRecallAt())
            .orElseThrow();

    assertThat(renderer.renderInputJsonl(replacement), containsString("opens at ten"));
  }

  @Test
  void renewsAQuestionFromAnOlderBatchResultWithoutRequestMetadata() throws Exception {
    RecallPrompt imported = importProviderQuestion(null);
    stubCurrentQuestion("Nine");

    var offered = controller.getRecallPrompt(reloadTracker());

    assertThat(offered.getId(), not(equalTo(imported.getId())));
    assertThat(offered.getMcq().getResponseChoices().getFirst(), is("Nine"));
    assertThat(synchronousApi.requestInputTexts(1).getFirst(), containsString(ORIGINAL));
  }

  @Test
  void reimportingTheSameBatchRowKeepsTheSavedQuestion() throws Exception {
    RecallPrompt imported = importProviderQuestion(renderRequestMetadata());
    QuestionGenerationBatchRequest importedRow = requests.findById(request.getId()).orElseThrow();

    assertThat(importedRow.getStatus(), is(QuestionGenerationBatchRequestStatus.IMPORTED));
    assertThat(rowImporter.importRow(importedRow), is(false));
    makeMe.entityPersister.flushAndClear();

    assertThat(savedPrompts(), hasSize(1));
    assertThat(controller.getRecallPrompt(reloadTracker()).getId(), is(imported.getId()));
  }

  private void stubCurrentQuestion(String answer) {
    synchronousApi.stubStructuredResponse(
        makeMe
            .aGeneratedMcq()
            .stem("According to the current note, when does it open?")
            .correctAnswer(answer)
            .please());
  }

  private String acceptedRevision() {
    return notebookGitBindings.findAcceptedRevision(tracker.getNote().getNotebook().getId());
  }
}

package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import com.odde.donut.controllers.dto.QuestionContestResult;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.exceptions.OpenAiNotAvailableException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.GlobalSettingsService;
import com.odde.donut.services.ai.QuestionEvaluation;
import com.openai.models.responses.StructuredResponseCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class RecallPromptContestControllerTest extends RecallPromptControllerTestBase {
  @Autowired GlobalSettingsService globalSettingsService;

  RecallPrompt recallPrompt;
  QuestionEvaluation questionEvaluation = new QuestionEvaluation();

  @BeforeEach
  void setUp() {
    questionEvaluation.correctChoices = new int[] {0};
    questionEvaluation.feasibleQuestion = true;
    questionEvaluation.improvementAdvices = "what a horrible question!";

    Note note = ownedNote();
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).please();
    recallPrompt = mcqPrompt(memoryTracker, note);
  }

  @Test
  void askWithNoteThatCannotAccess() {
    currentUser.setUser(null);
    assertThrows(ResponseStatusException.class, () -> controller.contest(recallPrompt));
  }

  @Test
  void shouldNotBeAbleToContestForOthersMemoryTracker() {
    MemoryTracker othersTracker = memoryTrackerOwnedByAnotherUser();
    RecallPrompt othersPrompt =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(othersTracker)
            .withMcqForNote(othersTracker.getNote())
            .please();
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.contest(othersPrompt));
  }

  @Test
  void rejected() throws UnexpectedNoAccessRightException {
    openAiStructuredResponseMock.stubStructuredResponse(questionEvaluation);

    QuestionContestResult contest = controller.contest(recallPrompt);
    assertTrue(contest.rejected);
    assertThat(recallPrompt.getMcq().isContested(), equalTo(false));
  }

  @Test
  void acceptTheContest() throws UnexpectedNoAccessRightException {
    globalSettingsService
        .globalSettingEvaluation()
        .setKeyValue(makeMe.aTimestamp().please(), "gpt-new");
    questionEvaluation.feasibleQuestion = false;
    openAiStructuredResponseMock.stubStructuredResponse(questionEvaluation);

    QuestionContestResult contestResult = controller.contest(recallPrompt);
    assertFalse(contestResult.rejected);
    assertThat(recallPrompt.getMcq().isContested(), equalTo(true));

    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<StructuredResponseCreateParams<QuestionEvaluation>> paramsCaptor =
        ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
    verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
    assertThat(
        paramsCaptor.getValue().rawParams().model().orElseThrow().asString(), equalTo("gpt-new"));
  }

  @Test
  void shouldThrowWhenOpenAiNotAvailable() {
    testabilitySettings.setOpenAiTokenOverride("");
    assertThrows(OpenAiNotAvailableException.class, () -> controller.contest(recallPrompt));
  }
}

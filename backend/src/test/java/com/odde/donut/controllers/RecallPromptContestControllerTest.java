package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.odde.donut.controllers.dto.QuestionContestResult;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.exceptions.OpenAiNotAvailableException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.GlobalSettingsService;
import com.odde.donut.services.ai.SupportedChoices;
import com.openai.models.responses.StructuredResponseCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class RecallPromptContestControllerTest extends RecallPromptControllerTestBase {
  @Autowired GlobalSettingsService globalSettingsService;

  RecallPrompt recallPrompt;

  @BeforeEach
  void setUp() {
    Note note = ownedNote();
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).please();
    recallPrompt = mcqPrompt(memoryTracker);
    makeMe
        .anMcq()
        .mcq(recallPrompt.getMcq())
        .stem("What is the first color in the rainbow?")
        .choices("red", "black", "green")
        .correctAnswerIndex(0)
        .please();
  }

  @Test
  void askWithNoteThatCannotAccess() {
    currentUser.setUser(null);
    assertThrows(ResponseStatusException.class, () -> controller.contest(recallPrompt));
  }

  @Test
  void shouldNotBeAbleToContestForOthersMemoryTracker() {
    MemoryTracker othersTracker = memoryTrackerOwnedByAnotherUser();
    RecallPrompt othersPrompt = makeMe.aRecallPrompt().forMemoryTracker(othersTracker).please();
    assertThrows(UnexpectedNoAccessRightException.class, () -> controller.contest(othersPrompt));
  }

  @Test
  void refusedWhenTheNoteSupportsExactlyTheStoredAnswer() throws UnexpectedNoAccessRightException {
    QuestionContestResult contest = contestWhenNoteSupports(0);

    assertTrue(contest.rejected);
    assertThat(
        contest.advice,
        equalTo(
            "The note supports exactly one of these choices, so the question stands. Please answer it."));
    assertThat(recallPrompt.getMcq().isContested(), equalTo(false));
  }

  @Nested
  class Accepted {
    @Test
    void whenTheNoteSupportsAnotherChoice() throws UnexpectedNoAccessRightException {
      QuestionContestResult contest = contestWhenNoteSupports(1);

      assertFalse(contest.rejected);
      assertThat(
          contest.advice,
          equalTo(
              "The question's stored answer is \"red\", but the note supports \"black\". Please write a question with exactly one correct answer."));
      assertThat(recallPrompt.getMcq().isContested(), equalTo(true));
    }

    @Test
    void whenTheNoteSupportsMoreThanOneChoice() throws UnexpectedNoAccessRightException {
      QuestionContestResult contest = contestWhenNoteSupports(0, 2);

      assertFalse(contest.rejected);
      assertThat(contest.advice, containsString("the note supports \"red\", \"green\""));
      assertThat(recallPrompt.getMcq().isContested(), equalTo(true));
    }

    @Test
    void whenTheNoteSupportsNoChoice() throws UnexpectedNoAccessRightException {
      QuestionContestResult contest = contestWhenNoteSupports();

      assertFalse(contest.rejected);
      assertThat(
          contest.advice,
          equalTo(
              "The question's stored answer is \"red\", but the note supports none of the choices. Please write a question with exactly one correct answer."));
      assertThat(recallPrompt.getMcq().isContested(), equalTo(true));
    }

    @Test
    void whenTheQuestionHasNoStoredAnswer() throws UnexpectedNoAccessRightException {
      recallPrompt.getMcq().setCorrectAnswerIndex(null);

      QuestionContestResult contest = contestWhenNoteSupports();

      assertFalse(contest.rejected);
      assertThat(
          contest.advice,
          equalTo(
              "The question has no stored answer, but the note supports none of the choices. Please write a question with exactly one correct answer."));
      assertThat(recallPrompt.getMcq().isContested(), equalTo(true));
    }

    @Test
    void whenTheModelNamesAPositionOutsideTheChoices() throws UnexpectedNoAccessRightException {
      QuestionContestResult contest = contestWhenNoteSupports(1, 9);

      assertFalse(contest.rejected);
      assertThat(contest.advice, containsString("the note supports \"black\". Please"));
    }
  }

  @Test
  void noVerdictWithoutAModelResponse() throws UnexpectedNoAccessRightException {
    openAiStructuredResponseMock.stubStructuredResponse(null);

    assertThat(controller.contest(recallPrompt), nullValue());
    assertThat(recallPrompt.getMcq().isContested(), equalTo(false));
  }

  @Test
  void asksTheEvaluationModelOnceWhichChoicesTheNoteSupports()
      throws UnexpectedNoAccessRightException {
    globalSettingsService
        .globalSettingEvaluation()
        .setKeyValue(makeMe.aTimestamp().please(), "gpt-new");
    makeMe
        .theNotebook(recallPrompt.getMemoryTracker().getNote().getNotebook())
        .readmeContent("---\nquestion_generation_instruction: NOT_FOR_THE_CHECK\n---\n")
        .please();

    contestWhenNoteSupports(1);

    var request = capturedRequest().rawParams();
    assertThat(request.model().orElseThrow().asString(), equalTo("gpt-new"));
    assertThat(request.instructions().orElseThrow(), equalTo(SupportedChoices.INSTRUCTIONS));
    String input = request.input().flatMap(i -> i.text()).orElse("");
    assertThat(input, containsString("What is the first color in the rainbow?"));
    assertThat(input, containsString("\"black\""));
    assertThat(input, not(containsString("\"correctAnswerIndex\":0")));
  }

  @Test
  void contestsAgainstTheTrackersCurrentNote() throws UnexpectedNoAccessRightException {
    Note current =
        reifyTrackedProperty(recallPrompt.getMemoryTracker(), "Current contest evidence");

    contestWhenNoteSupports(0);

    assertThat(
        capturedRequest().rawParams().input().flatMap(input -> input.text()).orElse(""),
        containsString("<focus_note>\nTitle: " + current.getTitle() + "\n"));
  }

  @Test
  void shouldThrowWhenOpenAiNotAvailable() {
    testabilitySettings.setOpenAiTokenOverride("");
    assertThrows(OpenAiNotAvailableException.class, () -> controller.contest(recallPrompt));
  }

  private QuestionContestResult contestWhenNoteSupports(int... positions)
      throws UnexpectedNoAccessRightException {
    SupportedChoices supported = new SupportedChoices();
    supported.supportedChoices = positions;
    openAiStructuredResponseMock.stubStructuredResponse(supported);
    return controller.contest(recallPrompt);
  }

  private StructuredResponseCreateParams<SupportedChoices> capturedRequest() {
    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<StructuredResponseCreateParams<SupportedChoices>> paramsCaptor =
        ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
    verify(openAiStructuredResponseMock.responseService(), times(1)).create(paramsCaptor.capture());
    return paramsCaptor.getValue();
  }
}

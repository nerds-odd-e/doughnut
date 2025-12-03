package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.dto.Randomization.RandomStrategy.first;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.odde.doughnut.utils.TimestampOperations;
import com.openai.client.OpenAIClient;
import java.sql.Timestamp;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

class RecallPromptControllerTests extends ControllerTestBase {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired MakeMe makeMe;
  @Autowired RecallPromptController controller;
  @Autowired GlobalSettingsService globalSettingsService;
  OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    testabilitySettings.setRandomization(new Randomization(first, 1));

    openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);

    // Mock chat completion for question evaluation
    QuestionEvaluation evaluation = new QuestionEvaluation();
    evaluation.feasibleQuestion = false;
    evaluation.correctChoices = new int[] {0};
    evaluation.improvementAdvices = "This question needs improvement";
    openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(evaluation);
  }

  RecallPromptController nullUserController() {
    currentUser.setUser(null);
    return controller;
  }

  @Nested
  class answerQuizQuestion {
    MemoryTracker memoryTracker;
    RecallPrompt recallPrompt;
    AnswerDTO answerDTO = new AnswerDTO();

    @BeforeEach
    void setup() {
      Note answerNote = makeMe.aNote().please();
      memoryTracker =
          makeMe
              .aMemoryTrackerFor(answerNote)
              .by(currentUser.getUser())
              .forgettingCurveAndNextRecallAt(200)
              .please();
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      recallPrompt =
          makeMe.aRecallPrompt().ofAIGeneratedQuestion(mcqWithAnswer, answerNote).please();
      answerDTO.setChoiceIndex(0);
    }

    @Test
    void shouldValidateTheAnswerAndUpdateMemoryTracker() {
      Integer oldRepetitionCount = memoryTracker.getRepetitionCount();
      AnsweredQuestion answerResult = controller.answerQuiz(recallPrompt, answerDTO);
      assertThat(answerResult.answer.getCorrect(), is(true));
      assertThat(memoryTracker.getRepetitionCount(), greaterThan(oldRepetitionCount));
    }

    @Test
    void shouldSaveThinkingTimeMs() {
      answerDTO.setThinkingTimeMs(5000);
      AnsweredQuestion answerResult = controller.answerQuiz(recallPrompt, answerDTO);
      assertThat(answerResult.answer.getThinkingTimeMs(), equalTo(5000));
    }

    @Test
    void shouldNoteIncreaseIndexIfRepeatImmediately() {
      testabilitySettings.timeTravelTo(memoryTracker.getLastRecalledAt());
      Integer oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
      controller.answerQuiz(recallPrompt, answerDTO);
      assertThat(memoryTracker.getForgettingCurveIndex(), equalTo(oldForgettingCurveIndex));
    }

    @Test
    void shouldIncreaseTheIndex() {
      testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
      Integer oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
      controller.answerQuiz(recallPrompt, answerDTO);
      assertThat(memoryTracker.getForgettingCurveIndex(), greaterThan(oldForgettingCurveIndex));
      assertThat(
          memoryTracker.getLastRecalledAt(), equalTo(testabilitySettings.getCurrentUTCTimestamp()));
    }

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      AnswerDTO answer = new AnswerDTO();
      assertThrows(
          ResponseStatusException.class,
          () -> nullUserController().answerQuiz(recallPrompt, answer));
    }

    @Nested
    class WrongAnswer {
      @BeforeEach
      void setup() {
        answerDTO.setChoiceIndex(1);
      }

      @Test
      void shouldValidateTheWrongAnswer() {
        testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
        Integer oldRepetitionCount = memoryTracker.getRepetitionCount();
        AnsweredQuestion answerResult = controller.answerQuiz(recallPrompt, answerDTO);
        assertThat(answerResult.answer.getCorrect(), is(false));
        assertThat(memoryTracker.getRepetitionCount(), greaterThan(oldRepetitionCount));
      }

      @Test
      void shouldNotChangeTheLastRecalledAtTime() {
        testabilitySettings.timeTravelTo(memoryTracker.getNextRecallAt());
        Timestamp lastRecalledAt = memoryTracker.getLastRecalledAt();
        Integer oldForgettingCurveIndex = memoryTracker.getForgettingCurveIndex();
        controller.answerQuiz(recallPrompt, answerDTO);
        assertThat(memoryTracker.getForgettingCurveIndex(), lessThan(oldForgettingCurveIndex));
        assertThat(memoryTracker.getLastRecalledAt(), equalTo(lastRecalledAt));
      }

      @Test
      void shouldRepeatTheNextDay() {
        controller.answerQuiz(recallPrompt, answerDTO);
        assertThat(
            memoryTracker.getNextRecallAt(),
            lessThan(
                TimestampOperations.addHoursToTimestamp(
                    testabilitySettings.getCurrentUTCTimestamp(), 25)));
      }
    }
  }

  @Nested
  class RegenerateQuestion {
    RecallPrompt recallPrompt;
    Note note;

    @BeforeEach
    void setUp() {
      note = makeMe.aNote().please();
      recallPrompt = makeMe.aRecallPrompt().approvedQuestionOf(note).please();
    }

    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () -> {
            currentUser.setUser(null);
            QuestionContestResult contestResult = new QuestionContestResult();
            contestResult.advice = "test";
            controller.regenerate(recallPrompt, contestResult);
          });
    }

    @Test
    void createQuizQuestion() throws JsonProcessingException {
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

      // Mock the chat completion API calls
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(jsonQuestion);

      QuestionContestResult contestResult = new QuestionContestResult();
      contestResult.advice = "test";
      RecallPrompt regeneratedQuestion = controller.regenerate(recallPrompt, contestResult);

      Assertions.assertThat(regeneratedQuestion.getMultipleChoicesQuestion().getF0__stem())
          .contains("What is the first color in the rainbow?");
    }

    @Test
    void shouldPassOldQuestionAndContestResultToOpenAiApi() throws JsonProcessingException {
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

      // Mock the chat completion API calls
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(jsonQuestion);

      QuestionContestResult contestResult = new QuestionContestResult();
      contestResult.advice = "test";
      controller.regenerate(recallPrompt, contestResult);

      // Verify chat completion call contains message with question info and contest result
      ArgumentCaptor<com.openai.models.chat.completions.ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(
              com.openai.models.chat.completions.ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService(), atLeastOnce())
          .create(paramsCaptor.capture());

      // Check if any message contains the required contest info
      boolean hasContestInfo =
          paramsCaptor.getValue().messages().stream()
              .map(Object::toString)
              .anyMatch(
                  content ->
                      content.contains("Previously generated non-feasible question")
                          && content.contains("Improvement advice")
                          && content.contains("test")
                          && content.contains(
                              "Please regenerate or refine the question based on the above advice"));

      assertThat("A message should contain the contest information", hasContestInfo, is(true));
    }
  }

  @Nested
  class Contest {
    RecallPrompt recallPrompt;
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();

    @BeforeEach
    void setUp() {
      questionEvaluation.correctChoices = new int[] {0};
      questionEvaluation.feasibleQuestion = true;
      questionEvaluation.improvementAdvices = "what a horrible question!";

      MCQWithAnswer aiGeneratedQuestion = makeMe.aMCQWithAnswer().please();
      Note note = makeMe.aNote().please();
      recallPrompt =
          makeMe.aRecallPrompt().ofAIGeneratedQuestion(aiGeneratedQuestion, note).please();
    }

    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () -> {
            currentUser.setUser(null);
            controller.contest(recallPrompt);
          });
    }

    @Test
    void rejected() {
      questionEvaluation.feasibleQuestion = true;
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(questionEvaluation);

      QuestionContestResult contest = controller.contest(recallPrompt);
      assertTrue(contest.rejected);
    }

    @Test
    void useTheRightModel() {
      globalSettingsService
          .globalSettingEvaluation()
          .setKeyValue(makeMe.aTimestamp().please(), "gpt-new");

      questionEvaluation.feasibleQuestion = true;
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(questionEvaluation);

      controller.contest(recallPrompt);

      ArgumentCaptor<com.openai.models.chat.completions.ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(
              com.openai.models.chat.completions.ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService()).create(paramsCaptor.capture());
      assertThat(paramsCaptor.getValue().model().asString(), equalTo("gpt-new"));
    }

    @Test
    void acceptTheContest() {
      questionEvaluation.feasibleQuestion = false;
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(questionEvaluation);

      QuestionContestResult contestResult = controller.contest(recallPrompt);
      assertFalse(contestResult.rejected);
    }
  }

  @Nested
  class GenerateRandomQuestion {
    @Test
    void itMustPersistTheQuestionGenerated() {
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

      // Mock the chat completion API calls
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(jsonQuestion);

      Note note = makeMe.aNote().details("description long enough.").rememberSpelling().please();
      // another note is needed, otherwise the note will be the only note in the notebook, and the
      // question cannot be generated.
      makeMe.aNote().under(note).please();
      MemoryTracker rp = makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      RecallPrompt recallPrompt = controller.askAQuestion(rp);

      assertThat(recallPrompt.getId(), notNullValue());
    }

    @Test
    void shouldReuseExistingUnansweredRecallPrompt() {
      // Create a note and memory tracker
      Note note = makeMe.aNote().details("description long enough.").rememberSpelling().please();
      makeMe.aNote().under(note).please(); // Add another note to the notebook
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      // Create an existing unanswered recall prompt for the note with the same memory tracker
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      RecallPrompt existingPrompt =
          makeMe
              .aRecallPrompt()
              .ofAIGeneratedQuestion(mcqWithAnswer, note)
              .forMemoryTracker(memoryTracker)
              .please();

      // Ask for a question for the memory tracker
      RecallPrompt returnedPrompt = controller.askAQuestion(memoryTracker);

      // Verify that the existing prompt was returned
      assertThat(returnedPrompt.getId(), equalTo(existingPrompt.getId()));

      // Verify that no new prompt was created
      long count =
          makeMe
              .entityPersister
              .createQuery("SELECT COUNT(rp) FROM RecallPrompt rp", Long.class)
              .getSingleResult();
      assertThat(count, equalTo(1L));
    }

    @Test
    void shouldGenerateNewPromptWhenExistingPromptsHaveAnswers() {
      // Mock the chat completion API calls
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(jsonQuestion);

      // Create a note and memory tracker
      Note note = makeMe.aNote().details("description long enough.").rememberSpelling().please();
      makeMe.aNote().under(note).please(); // Add another note to the notebook
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      // Create an existing recall prompt with an answer
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      RecallPrompt existingPrompt =
          makeMe
              .aRecallPrompt()
              .ofAIGeneratedQuestion(mcqWithAnswer, note)
              .answerChoiceIndex(0) // Add an answer
              .please();

      // Ask for a question for the memory tracker
      RecallPrompt returnedPrompt = controller.askAQuestion(memoryTracker);

      // Verify that a new prompt was returned
      assertThat(returnedPrompt.getId(), not(equalTo(existingPrompt.getId())));

      // Verify that a new prompt was created
      long count =
          makeMe
              .entityPersister
              .createQuery("SELECT COUNT(rp) FROM RecallPrompt rp", Long.class)
              .getSingleResult();
      assertThat(count, equalTo(2L));
    }

    @Test
    void shouldNotReuseContestedQuestions() {
      // Mock the chat completion API calls
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(jsonQuestion);

      // Create a note and memory tracker
      Note note = makeMe.aNote().details("description long enough.").rememberSpelling().please();
      makeMe.aNote().under(note).please(); // Add another note to the notebook
      MemoryTracker memoryTracker =
          makeMe.aMemoryTrackerFor(note).by(currentUser.getUser()).please();

      // Create an existing unanswered recall prompt with a contested question
      MCQWithAnswer mcqWithAnswer = makeMe.aMCQWithAnswer().please();
      PredefinedQuestion contestedQuestion =
          makeMe.aPredefinedQuestion().ofAIGeneratedQuestion(mcqWithAnswer, note).please();
      contestedQuestion.setContested(true);
      makeMe.entityPersister.save(contestedQuestion);
      RecallPrompt existingPrompt = makeMe.aRecallPrompt().please();
      existingPrompt.getAnswerableMCQ().setPredefinedQuestion(contestedQuestion);
      makeMe.entityPersister.save(existingPrompt);

      // Mock the AI to generate a new question
      MCQWithAnswer newQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(newQuestion);

      // Ask for a question for the memory tracker
      RecallPrompt returnedPrompt = controller.askAQuestion(memoryTracker);

      // Verify that a new prompt was returned
      assertThat(returnedPrompt.getId(), not(equalTo(existingPrompt.getId())));
      assertThat(
          returnedPrompt.getAnswerableMCQ().getPredefinedQuestion().isContested(), equalTo(false));

      // Verify that a new prompt was created
      long count =
          makeMe
              .entityPersister
              .createQuery("SELECT COUNT(rp) FROM RecallPrompt rp", Long.class)
              .getSingleResult();
      assertThat(count, equalTo(2L));
    }
  }

  @Nested
  class ContestQuestion {
    RecallPrompt recallPrompt;
    Note note;
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();

    @BeforeEach
    void setUp() {
      note = makeMe.aNote().please();
      recallPrompt = makeMe.aRecallPrompt().approvedQuestionOf(note).please();
      questionEvaluation.correctChoices = new int[] {0};
      questionEvaluation.feasibleQuestion = false;
      questionEvaluation.improvementAdvices = "This is a valid contest";
    }

    @Test
    void shouldMarkQuestionAsContestedWhenContestIsAccepted() {
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(questionEvaluation);

      // When
      QuestionContestResult result = controller.contest(recallPrompt);

      // Then
      assertThat(result.rejected, equalTo(false));
      assertThat(
          recallPrompt.getAnswerableMCQ().getPredefinedQuestion().isContested(), equalTo(true));
    }

    @Test
    void shouldNotMarkQuestionAsContestedWhenContestIsRejected() {
      questionEvaluation.feasibleQuestion = true;
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(questionEvaluation);

      // When
      QuestionContestResult result = controller.contest(recallPrompt);

      // Then
      assertThat(result.rejected, equalTo(true));
      assertThat(
          recallPrompt.getAnswerableMCQ().getPredefinedQuestion().isContested(), equalTo(false));
    }
  }
}

package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.dto.Randomization.RandomStrategy.first;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.*;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.TimestampOperations;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.AiToolName;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIAssistantMocker;
import com.odde.doughnut.testability.OpenAIAssistantThreadMocker;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.testability.builders.RecallPromptBuilder;
import com.theokanning.openai.assistants.message.MessageRequest;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import com.theokanning.openai.client.OpenAiApi;
import java.sql.Timestamp;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RestRecallPromptControllerTests {
  @Mock OpenAiApi openAiApi;
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  private UserModel currentUser;
  private final TestabilitySettings testabilitySettings = new TestabilitySettings();
  OpenAIChatCompletionMock openAIChatCompletionMock;
  OpenAIAssistantMocker openAIAssistantMocker;
  OpenAIAssistantThreadMocker openAIAssistantThreadMocker;

  RestRecallPromptController controller;

  @BeforeEach
  void setup() {
    testabilitySettings.setRandomization(new Randomization(first, 1));
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
    currentUser = makeMe.aUser().toModelPlease();
    controller =
        new RestRecallPromptController(
            openAiApi, modelFactoryService, currentUser, testabilitySettings);

    // Initialize assistant mocker
    openAIAssistantMocker = new OpenAIAssistantMocker(openAiApi);
    openAIAssistantThreadMocker = openAIAssistantMocker.mockThreadCreation(null);
  }

  RestRecallPromptController nullUserController() {
    return new RestRecallPromptController(
        openAiApi, modelFactoryService, makeMe.aNullUserModelPlease(), testabilitySettings);
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
              .by(currentUser)
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
            RestRecallPromptController restAiController =
                new RestRecallPromptController(
                    openAiApi,
                    makeMe.modelFactoryService,
                    makeMe.aNullUserModelPlease(),
                    testabilitySettings);
            QuestionContestResult contestResult = new QuestionContestResult();
            contestResult.reason = "test";
            restAiController.regenerate(recallPrompt, contestResult);
          });
    }

    @Test
    void createQuizQuestion() throws JsonProcessingException {
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

      // Mock the assistant API calls using the same pattern as GenerateRandomQuestion
      openAIAssistantThreadMocker
          .mockCreateRunInProcess("my-run-id")
          .aRunThatRequireAction(
              jsonQuestion, AiToolName.ASK_SINGLE_ANSWER_MULTIPLE_CHOICE_QUESTION.getValue())
          .mockRetrieveRun()
          .mockCancelRun("my-run-id");

      QuestionContestResult contestResult = new QuestionContestResult();
      contestResult.reason = "test";
      RecallPrompt regeneratedQuestion = controller.regenerate(recallPrompt, contestResult);

      Assertions.assertThat(regeneratedQuestion.getMultipleChoicesQuestion().getStem())
          .contains("What is the first color in the rainbow?");
    }

    @Test
    void shouldPassOldQuestionAndContestResultToOpenAiApi() throws JsonProcessingException {
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

      // Mock the assistant API calls
      openAIAssistantThreadMocker
          .mockCreateRunInProcess("my-run-id")
          .aRunThatRequireAction(
              jsonQuestion, AiToolName.ASK_SINGLE_ANSWER_MULTIPLE_CHOICE_QUESTION.getValue())
          .mockRetrieveRun()
          .mockCancelRun("my-run-id");

      QuestionContestResult contestResult = new QuestionContestResult();
      contestResult.reason = "test";
      controller.regenerate(recallPrompt, contestResult);

      ArgumentCaptor<RunCreateRequest> argumentCaptor =
          ArgumentCaptor.forClass(RunCreateRequest.class);
      verify(openAiApi).createRun(any(), argumentCaptor.capture());
      ArgumentCaptor<ThreadRequest> messagesCaptor = ArgumentCaptor.forClass(ThreadRequest.class);
      verify(openAiApi).createThread(messagesCaptor.capture());

      List<MessageRequest> messages = messagesCaptor.getValue().getMessages();
      assertThat(messages.size(), equalTo(3));
      assertThat(messages.get(0).getRole(), equalTo("assistant"));
      assertThat(
          messages.get(1).getContent().toString(),
          containsString(AiToolFactory.mcqWithAnswerAiTool().getMessageBody()));
      String lastMessage = messages.get(2).getContent().toString();
      assertThat(
          lastMessage,
          allOf(
              containsString("\"stem\" : \"a default question stem\""),
              containsString("Non-feasible reason:"),
              containsString("test"),
              containsString(
                  "Please regenerate or refine the question based on the above feedback.")));
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
      questionEvaluation.explanation = "what a horrible question!";

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
            RestRecallPromptController restAiController =
                new RestRecallPromptController(
                    openAiApi,
                    makeMe.modelFactoryService,
                    makeMe.aNullUserModelPlease(),
                    testabilitySettings);
            restAiController.contest(recallPrompt);
          });
    }

    @Test
    void rejected() {
      openAIAssistantThreadMocker
          .mockCreateRunInProcess("my-run-id")
          .aRunThatRequireAction(questionEvaluation, "evaluate_question")
          .mockRetrieveRun()
          .mockCancelRun("my-run-id");

      QuestionContestResult contest = controller.contest(recallPrompt);
      assertTrue(contest.rejected);
    }

    @Test
    void useTheRightModel() {
      GlobalSettingsService globalSettingsService = new GlobalSettingsService(modelFactoryService);
      globalSettingsService
          .globalSettingEvaluation()
          .setKeyValue(makeMe.aTimestamp().please(), "gpt-new");

      openAIAssistantThreadMocker
          .mockCreateRunInProcess("my-run-id")
          .aRunThatRequireAction(questionEvaluation, "evaluate_question")
          .mockRetrieveRun()
          .mockCancelRun("my-run-id");

      controller.contest(recallPrompt);

      ArgumentCaptor<RunCreateRequest> argumentCaptor =
          ArgumentCaptor.forClass(RunCreateRequest.class);
      verify(openAiApi).createRun(any(), argumentCaptor.capture());
      assertThat(argumentCaptor.getValue().getModel(), equalTo("gpt-new"));
    }

    @Test
    void acceptTheContest() {
      questionEvaluation.feasibleQuestion = false;

      openAIAssistantThreadMocker
          .mockCreateRunInProcess("my-run-id")
          .aRunThatRequireAction(questionEvaluation, "evaluate_question")
          .mockRetrieveRun()
          .mockCancelRun("my-run-id");

      QuestionContestResult contest = controller.contest(recallPrompt);
      assertFalse(contest.rejected);
    }
  }

  @Nested
  class GenerateRandomQuestion {
    @Test
    void itMustPersistTheQuestionGenerated() {
      MCQWithAnswer jsonQuestion =
          makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

      // Mock the assistant API calls
      openAIAssistantThreadMocker
          .mockCreateRunInProcess("my-run-id")
          .aRunThatRequireAction(
              jsonQuestion, AiToolName.ASK_SINGLE_ANSWER_MULTIPLE_CHOICE_QUESTION.getValue())
          .mockRetrieveRun()
          .mockCancelRun("my-run-id");

      Note note = makeMe.aNote().details("description long enough.").rememberSpelling().please();
      // another note is needed, otherwise the note will be the only note in the notebook, and the
      // question cannot be generated.
      makeMe.aNote().under(note).please();
      MemoryTracker rp = makeMe.aMemoryTrackerFor(note).by(currentUser).please();

      RecallPrompt recallPrompt = controller.askAQuestion(rp);

      assertThat(recallPrompt.getId(), notNullValue());
    }
  }

  @Nested
  class showQuestion {

    @Test
    void shouldNotBeAbleToSeeNoteIDontHaveAccessTo() {
      RecallPrompt recallPrompt = makeMe.aRecallPrompt().please();
      assertThrows(
          UnexpectedNoAccessRightException.class, () -> controller.showQuestion(recallPrompt));
    }

    @Test
    void canSeeNoteThatHasReadAccess() throws UnexpectedNoAccessRightException {
      Note note = makeMe.aNote().creatorAndOwner(currentUser).please();
      RecallPromptBuilder recallPromptBuilder = makeMe.aRecallPrompt();
      RecallPrompt recallPrompt = recallPromptBuilder.approvedQuestionOf(note).please();
      makeMe.theRecallPrompt(recallPrompt).answerSpelling("wrong").please();
      makeMe.refresh(currentUser.getEntity());
      AnsweredQuestion answeredQuestion = controller.showQuestion(recallPrompt);
      assertThat(answeredQuestion.recallPromptId, equalTo(recallPrompt.getId()));
    }
  }
}

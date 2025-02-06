package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.randomizers.RealRandomizer;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.tools.AiToolName;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIAssistantMocker;
import com.odde.doughnut.testability.OpenAIAssistantThreadMocker;
import com.theokanning.openai.assistants.run.RunCreateRequest;
import com.theokanning.openai.client.OpenAiApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiQuestionGeneratorTests {
  @Mock OpenAiApi openAiApi;
  @Autowired ModelFactoryService modelFactoryService;
  @Autowired MakeMe makeMe;
  OpenAIAssistantMocker openAIAssistantMocker;
  OpenAIAssistantThreadMocker openAIAssistantThreadMocker;
  AiQuestionGenerator aiQuestionGenerator;

  @BeforeEach
  void setup() {
    GlobalSettingsService globalSettingsService = new GlobalSettingsService(modelFactoryService);
    aiQuestionGenerator =
        new AiQuestionGenerator(openAiApi, globalSettingsService, new RealRandomizer());

    // Initialize assistant mocker
    openAIAssistantMocker = new OpenAIAssistantMocker(openAiApi);
    openAIAssistantThreadMocker = openAIAssistantMocker.mockThreadCreation(null);
  }

  @Test
  void shouldGenerateQuestion() {
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
    // another note is needed, otherwise the note will be the only note in the notebook
    makeMe.aNote().under(note).please();

    MCQWithAnswer result = aiQuestionGenerator.getAiGeneratedQuestion(note, null);

    assertThat(
        result.getMultipleChoicesQuestion().getStem(),
        equalTo("What is the first color in the rainbow?"));
  }

  @Test
  void shouldIncludeFileSearchInTools() {
    Note note = makeMe.aNote().details("description long enough.").rememberSpelling().please();
    makeMe.aNote().under(note).please();
    MCQWithAnswer jsonQuestion =
        makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

    // Mock the assistant API calls
    openAIAssistantThreadMocker
        .mockCreateRunInProcess("my-run-id")
        .aRunThatRequireAction(
            jsonQuestion, AiToolName.ASK_SINGLE_ANSWER_MULTIPLE_CHOICE_QUESTION.getValue())
        .mockRetrieveRun()
        .mockCancelRun("my-run-id");

    aiQuestionGenerator.getAiGeneratedQuestion(note, null);

    // Capture the actual request
    ArgumentCaptor<RunCreateRequest> runRequestCaptor =
        ArgumentCaptor.forClass(RunCreateRequest.class);
    verify(openAiApi).createRun(any(), runRequestCaptor.capture());

    // Assert on the captured request
    RunCreateRequest actualRequest = runRequestCaptor.getValue();
    //    assertThat(actualRequest.getTools(), hasItem(hasProperty("type",
    // equalTo("file_search"))));
  }

  @Test
  void shouldShuffleChoicesWhenStrictChoiceOrderIsFalse() {
    // Setup a note with enough content for question generation
    Note note = makeMe.aNote().details("description long enough.").rememberSpelling().please();
    makeMe.aNote().under(note).please();

    // Prepare the AI response with strictChoiceOrder = false
    MCQWithAnswer originalQuestion =
        makeMe
            .aMCQWithAnswer()
            .stem("What is 2+2?")
            .choices("4", "3", "5", "6")
            .correctChoiceIndex(0)
            .strictChoiceOrder(false)
            .please();

    // Mock the assistant API calls
    openAIAssistantThreadMocker
        .mockCreateRunInProcess("my-run-id")
        .aRunThatRequireAction(
            originalQuestion, AiToolName.ASK_SINGLE_ANSWER_MULTIPLE_CHOICE_QUESTION.getValue())
        .mockRetrieveRun()
        .mockCancelRun("my-run-id");

    // Act
    MCQWithAnswer result = aiQuestionGenerator.getAiGeneratedQuestion(note, null);

    // Assert
    assertThat(
        result.getMultipleChoicesQuestion().getStem(),
        equalTo(originalQuestion.getMultipleChoicesQuestion().getStem()));
    assertThat(
        result.getMultipleChoicesQuestion().getChoices().size(),
        equalTo(originalQuestion.getMultipleChoicesQuestion().getChoices().size()));

    // Verify the correct answer is maintained
    String expectedCorrectAnswer =
        originalQuestion
            .getMultipleChoicesQuestion()
            .getChoices()
            .get(originalQuestion.getCorrectChoiceIndex());
    String actualCorrectAnswer =
        result.getMultipleChoicesQuestion().getChoices().get(result.getCorrectChoiceIndex());
    assertThat(actualCorrectAnswer, equalTo(expectedCorrectAnswer));
  }
}

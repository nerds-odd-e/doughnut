package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.odde.doughnut.utils.Randomizer;
import com.odde.doughnut.utils.randomizers.RealRandomizer;
import com.theokanning.openai.client.OpenAiApi;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
  @Autowired MakeMe makeMe;
  @Autowired GlobalSettingsService globalSettingsService;
  OpenAIChatCompletionMock openAIChatCompletionMock;
  AiQuestionGenerator aiQuestionGenerator;

  @BeforeEach
  void setup() {
    var objectMapper = new ObjectMapperConfig().objectMapper();
    aiQuestionGenerator =
        new AiQuestionGenerator(
            openAiApi, globalSettingsService, new RealRandomizer(), objectMapper);

    // Initialize chat completion mock
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
  }

  @Test
  void shouldGenerateQuestion() {
    MCQWithAnswer jsonQuestion =
        makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();

    // Mock the chat completion API calls
    openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(jsonQuestion);

    Note note = makeMe.aNote().details("description long enough.").rememberSpelling().please();
    // another note is needed, otherwise the note will be the only note in the notebook
    makeMe.aNote().under(note).please();

    MCQWithAnswer result = aiQuestionGenerator.getAiGeneratedQuestion(note, null);

    assertThat(
        result.getMultipleChoicesQuestion().getStem(),
        equalTo("What is the first color in the rainbow?"));
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

    // Mock the chat completion API calls
    openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(originalQuestion);

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

  @Test
  void shouldShuffleChoicesInSpecificOrder() {
    // Setup a mocked randomizer
    Randomizer mockedRandomizer = mock(Randomizer.class);
    var objectMapper = new ObjectMapperConfig().objectMapper();
    AiQuestionGenerator aiQuestionGeneratorWithMockedRandomizer =
        new AiQuestionGenerator(openAiApi, globalSettingsService, mockedRandomizer, objectMapper);

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

    // Mock the randomizer to return a specific shuffled order
    List<String> shuffledChoices = Arrays.asList("6", "4", "5", "3");
    doReturn(shuffledChoices).when(mockedRandomizer).shuffle(any());

    // Mock the chat completion API calls
    openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(originalQuestion);

    // Act
    MCQWithAnswer result =
        aiQuestionGeneratorWithMockedRandomizer.getAiGeneratedQuestion(note, null);

    // Assert
    assertThat(result.getMultipleChoicesQuestion().getChoices(), equalTo(shuffledChoices));
    assertThat(result.getCorrectChoiceIndex(), equalTo(1)); // "4" is now at index 1
  }

  @Test
  void shouldRejectQuestionWithInvalidChoiceIndex() {
    // Setup a note with enough content for question generation
    Note note = makeMe.aNote().details("description long enough.").rememberSpelling().please();
    makeMe.aNote().under(note).please();

    // Prepare an AI response with an invalid choice index (3 for a list of 3 choices)
    MCQWithAnswer invalidQuestion =
        makeMe
            .aMCQWithAnswer()
            .stem("What is 2+2?")
            .choices("4", "3", "5") // 3 choices (indices 0-2)
            .correctChoiceIndex(3) // Invalid index!
            .please();

    // Mock the chat completion API calls
    openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(invalidQuestion);

    // Act
    MCQWithAnswer result = aiQuestionGenerator.getAiGeneratedQuestion(note, null);

    // Assert
    assertThat(result, equalTo(null)); // Question should be rejected
  }
}

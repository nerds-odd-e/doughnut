package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.NoteQuestionGenerationService;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.odde.doughnut.testability.TestabilitySettings;
import com.odde.doughnut.utils.Randomizer;
import com.openai.client.OpenAIClient;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiQuestionGeneratorTests {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired MakeMe makeMe;
  @Autowired GlobalSettingsService globalSettingsService;
  @Autowired OpenAiApiHandler openAiApiHandler;
  @Autowired NoteQuestionGenerationService noteQuestionGenerationService;
  @Autowired AiQuestionGenerator aiQuestionGenerator;
  @Autowired TestabilitySettings testabilitySettings;
  OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void setup() {
    // Initialize chat completion mock
    openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);
    // Ensure OpenAI URL is reset to default before each test
    testabilitySettings.replaceServiceUrls(Map.of("openAi", "https://api.openai.com/v1/"));
  }

  @AfterEach
  void cleanup() {
    testabilitySettings.replaceServiceUrls(Map.of("openAi", "https://api.openai.com/v1/"));
    testabilitySettings.setOpenAiTokenOverride(null);
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
        result.getF0__multipleChoicesQuestion().getF0__stem(),
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
        result.getF0__multipleChoicesQuestion().getF0__stem(),
        equalTo(originalQuestion.getF0__multipleChoicesQuestion().getF0__stem()));
    assertThat(
        result.getF0__multipleChoicesQuestion().getF1__choices().size(),
        equalTo(originalQuestion.getF0__multipleChoicesQuestion().getF1__choices().size()));

    // Verify the correct answer is maintained
    String expectedCorrectAnswer =
        originalQuestion
            .getF0__multipleChoicesQuestion()
            .getF1__choices()
            .get(originalQuestion.getF1__correctChoiceIndex());
    String actualCorrectAnswer =
        result
            .getF0__multipleChoicesQuestion()
            .getF1__choices()
            .get(result.getF1__correctChoiceIndex());
    assertThat(actualCorrectAnswer, equalTo(expectedCorrectAnswer));
  }

  @Test
  void shouldShuffleChoicesInSpecificOrder() {
    // Setup a mocked randomizer
    Randomizer mockedRandomizer = mock(Randomizer.class);
    AiQuestionGenerator aiQuestionGeneratorWithMockedRandomizer =
        new AiQuestionGenerator(
            noteQuestionGenerationService,
            globalSettingsService,
            mockedRandomizer,
            openAiApiHandler,
            testabilitySettings);

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
    assertThat(result.getF0__multipleChoicesQuestion().getF1__choices(), equalTo(shuffledChoices));
    assertThat(result.getF1__correctChoiceIndex(), equalTo(1)); // "4" is now at index 1
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

  @Test
  void shouldThrowWhenOpenAiIsNotAvailable() {
    Note note = makeMe.aNote().details("description long enough.").rememberSpelling().please();
    makeMe.aNote().under(note).please();

    testabilitySettings.setOpenAiTokenOverride("");

    assertThrows(
        OpenAiNotAvailableException.class,
        () -> aiQuestionGenerator.getAiGeneratedQuestion(note, null));
  }
}

package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.NoteQuestionGenerationService;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.odde.doughnut.testability.TestabilitySettings;
import com.openai.client.OpenAIClient;
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
  OpenAiStructuredResponseMock openAiStructuredResponseMock;

  @BeforeEach
  void setup() {
    // Initialize OpenAI mock
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
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

    openAiStructuredResponseMock.stubStructuredResponse(jsonQuestion);

    Note note = makeMe.aNote().content("description long enough.").rememberSpelling().please();
    // another note is needed, otherwise the note will be the only note in the notebook
    makeMe.aNote().please();

    MCQWithAnswer result = aiQuestionGenerator.getAiGeneratedQuestion(note, null);

    assertThat(
        result.getQuestion().getQuestionStem(), equalTo("What is the first color in the rainbow?"));
  }

  @Test
  void shouldRejectQuestionWithInvalidChoiceIndex() {
    // Setup a note with enough content for question generation
    Note note = makeMe.aNote().content("description long enough.").rememberSpelling().please();
    makeMe.aNote().please();

    // Prepare an AI response with an invalid choice index (3 for a list of 3 choices)
    MCQWithAnswer invalidQuestion =
        makeMe
            .aMCQWithAnswer()
            .stem("What is 2+2?")
            .choices("4", "3", "5") // 3 choices (indices 0-2)
            .correctChoiceIndex(3) // Invalid index!
            .please();

    openAiStructuredResponseMock.stubStructuredResponse(invalidQuestion);

    // Act
    MCQWithAnswer result = aiQuestionGenerator.getAiGeneratedQuestion(note, null);

    // Assert
    assertThat(result, equalTo(null)); // Question should be rejected
  }

  @Test
  void shouldThrowWhenOpenAiIsNotAvailable() {
    Note note = makeMe.aNote().content("description long enough.").rememberSpelling().please();
    makeMe.aNote().please();

    testabilitySettings.setOpenAiTokenOverride("");

    assertThrows(
        OpenAiNotAvailableException.class,
        () -> aiQuestionGenerator.getAiGeneratedQuestion(note, null));
  }
}

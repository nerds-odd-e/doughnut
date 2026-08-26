package com.odde.donut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.OpenAiNotAvailableException;
import com.odde.donut.testability.MakeMe;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import com.odde.donut.testability.TestabilitySettings;
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
  @Autowired AiQuestionGenerator aiQuestionGenerator;
  @Autowired TestabilitySettings testabilitySettings;
  OpenAiStructuredResponseMock openAiStructuredResponseMock;

  @BeforeEach
  void setup() {
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
    testabilitySettings.replaceServiceUrls(Map.of("openAi", "https://api.openai.com/v1/"));
  }

  @AfterEach
  void cleanup() {
    testabilitySettings.replaceServiceUrls(Map.of("openAi", "https://api.openai.com/v1/"));
    testabilitySettings.setOpenAiTokenOverride(null);
  }

  private Note noteReadyForQuestionGeneration() {
    Note note = makeMe.aNote().content("description long enough.").please();
    makeMe.aNote().please();
    return note;
  }

  @Test
  void shouldGenerateQuestion() {
    GeneratedMcq jsonQuestion =
        makeMe.aGeneratedMcq().stem("What is the first color in the rainbow?").please();
    openAiStructuredResponseMock.stubStructuredResponse(jsonQuestion);

    Note note = noteReadyForQuestionGeneration();
    Mcq result = aiQuestionGenerator.getAiGeneratedQuestion(note, null);

    assertThat(result.getQuestionStem(), equalTo("What is the first color in the rainbow?"));
    assertThat(result.getNote(), equalTo(note));
  }

  @Test
  void shouldRejectQuestionWithBlankCorrectAnswer() {
    GeneratedMcq invalidQuestion =
        makeMe.aGeneratedMcq().stem("What is 2+2?").correctAnswer("").please();
    openAiStructuredResponseMock.stubStructuredResponse(invalidQuestion);

    assertThat(
        aiQuestionGenerator.getAiGeneratedQuestion(noteReadyForQuestionGeneration(), null),
        nullValue());
  }

  @Test
  void shouldThrowWhenOpenAiIsNotAvailable() {
    Note note = noteReadyForQuestionGeneration();
    testabilitySettings.setOpenAiTokenOverride("");

    assertThrows(
        OpenAiNotAvailableException.class,
        () -> aiQuestionGenerator.getAiGeneratedQuestion(note, null));
  }
}

package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.AiNoteAutomationService;
import com.odde.doughnut.services.ai.TitleReplacement;
import com.odde.doughnut.services.ai.UnderstandingChecklist;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import java.util.List;
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
class NoteAutomationServiceTests {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired MakeMe makeMe;
  @Autowired FocusContextRetrievalService focusContextRetrievalService;
  @Autowired FocusContextMarkdownRenderer focusContextMarkdownRenderer;
  @Autowired GlobalSettingsService globalSettingsService;
  @Autowired OpenAiApiHandler openAiApiHandler;
  OpenAiStructuredResponseMock openAiStructuredResponseMock;
  private Note testNote;
  private NoteAutomationService service;

  @BeforeEach
  void setup() {
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);

    testNote = makeMe.aNote().content("description long enough.").please();
    makeMe.aNote().please();

    AiNoteAutomationService aiNoteAutomationService =
        new AiNoteAutomationService(
            openAiApiHandler,
            globalSettingsService,
            focusContextRetrievalService,
            focusContextMarkdownRenderer,
            testNote);
    service = new NoteAutomationService(aiNoteAutomationService);
  }

  @Test
  void shouldHandleNoToolCallWhenSuggestingTitle() throws JsonProcessingException {
    openAiStructuredResponseMock.stubStructuredResponse(null);

    String result = service.suggestTitle();

    assertThat(result, is(nullValue()));
  }

  @Test
  void shouldReturnSuggestedTitle() throws JsonProcessingException {
    TitleReplacement titleReplacement = new TitleReplacement();
    titleReplacement.setNewTitle("Suggested Title");
    openAiStructuredResponseMock.stubStructuredResponse(titleReplacement);

    String result = service.suggestTitle();

    assertThat(result, is("Suggested Title"));
  }

  @Test
  void shouldReturnUnderstandingPoints() throws JsonProcessingException {
    UnderstandingChecklist understandingChecklist = new UnderstandingChecklist();
    understandingChecklist.setPoints(
        List.of(
            "English is a language that is spoken in many countries.",
            "It is also the most widely spoken language in the world."));
    openAiStructuredResponseMock.stubStructuredResponse(understandingChecklist);

    List<String> result = service.generateUnderstandingChecklist();

    assertThat(result, hasSize(2));
    assertThat(
        result,
        contains(
            "English is a language that is spoken in many countries.",
            "It is also the most widely spoken language in the world."));
  }

  @Test
  void shouldHandleNoToolCallWhenGeneratingUnderstandingChecklist() throws JsonProcessingException {
    openAiStructuredResponseMock.stubStructuredResponse(null);

    List<String> result = service.generateUnderstandingChecklist();

    assertThat(result, is(empty()));
  }

  @Test
  void shouldReturnEmptyListWhenChecklistIsNull() throws JsonProcessingException {
    openAiStructuredResponseMock.stubStructuredResponse(null);

    List<String> result = service.generateUnderstandingChecklist();

    assertThat(result, is(empty()));
  }
}

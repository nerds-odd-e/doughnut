package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.AiNoteAutomationService;
import com.odde.doughnut.services.ai.NoteRefinementLayout;
import com.odde.doughnut.services.ai.NoteRefinementLayoutItem;
import com.odde.doughnut.services.ai.TitleReplacement;
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
  void shouldReturnNoteRefinementLayout() throws JsonProcessingException {
    NoteRefinementLayout layout = new NoteRefinementLayout();
    layout.setItems(
        List.of(
            new NoteRefinementLayoutItem(
                "p1", "English is a language that is spoken in many countries.", false, List.of()),
            new NoteRefinementLayoutItem(
                "p2",
                "It is also the most widely spoken language in the world.",
                false,
                List.of())));
    openAiStructuredResponseMock.stubStructuredResponse(layout);

    NoteRefinementLayout result = service.generateRefinementSuggestions();

    assertThat(result.getItems(), hasSize(2));
    assertThat(
        result.getItems().stream().map(NoteRefinementLayoutItem::getText).toList(),
        contains(
            "English is a language that is spoken in many countries.",
            "It is also the most widely spoken language in the world."));
  }

  @Test
  void shouldReturnEmptyLayoutWhenNoResponse() throws JsonProcessingException {
    openAiStructuredResponseMock.stubStructuredResponse(null);

    NoteRefinementLayout result = service.generateRefinementSuggestions();

    assertThat(result.getItems(), is(empty()));
  }
}

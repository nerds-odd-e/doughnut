package com.odde.donut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.donut.entities.Note;
import com.odde.donut.services.GlobalSettingsService;
import com.odde.donut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.donut.services.focusContext.FocusContextRetrievalService;
import com.odde.donut.services.openAiApis.OpenAiApiHandler;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import com.odde.donut.testability.SpringTestBase;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AiNoteAutomationServiceTest extends SpringTestBase {
  @Autowired GlobalSettingsService globalSettingsService;
  @Autowired FocusContextRetrievalService focusContextRetrievalService;
  @Autowired FocusContextMarkdownRenderer focusContextMarkdownRenderer;
  @Autowired OpenAiApiHandler openAiApiHandler;
  OpenAiStructuredResponseMock openAiStructuredResponseMock;
  private AiNoteAutomationService service;

  @BeforeEach
  void setup() {
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);

    Note testNote = makeMe.aNote().content("description long enough.").please();
    makeMe.aNote().please();

    service =
        new AiNoteAutomationService(
            openAiApiHandler,
            globalSettingsService,
            focusContextRetrievalService,
            focusContextMarkdownRenderer,
            testNote);
  }

  @Nested
  class GenerateNoteRefinementLayout {
    @Test
    void shouldReturnNoteRefinementLayout() throws JsonProcessingException {
      NoteRefinementLayout layout = new NoteRefinementLayout();
      layout.setItems(
          List.of(
              NoteRefinementLayoutItems.leaf(
                  "p1", "English is a language that is spoken in many countries."),
              NoteRefinementLayoutItems.leaf(
                  "p2", "It is also the most widely spoken language in the world.")));
      openAiStructuredResponseMock.stubStructuredResponse(layout);

      NoteRefinementLayout result = service.generateRefinementSuggestions(null);

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

      assertThat(service.generateRefinementSuggestions(null).getItems(), empty());
    }
  }
}

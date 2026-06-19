package com.odde.doughnut.services.ai;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer;
import com.odde.doughnut.services.focusContext.FocusContextRetrievalService;
import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.odde.doughnut.testability.TestabilitySettings;
import com.openai.client.OpenAIClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiNoteAutomationServiceTest {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired MakeMe makeMe;
  @Autowired GlobalSettingsService globalSettingsService;
  @Autowired FocusContextRetrievalService focusContextRetrievalService;
  @Autowired FocusContextMarkdownRenderer focusContextMarkdownRenderer;
  @Autowired OpenAiApiHandler openAiApiHandler;
  @Autowired TestabilitySettings testabilitySettings;
  OpenAiStructuredResponseMock openAiStructuredResponseMock;
  private Note testNote;
  private AiNoteAutomationService service;

  @BeforeEach
  void setup() {
    testabilitySettings.setOpenAiTokenOverride(null);
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);

    testNote = makeMe.aNote().content("description long enough.").please();
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
              new NoteRefinementLayoutItem(
                  "p1",
                  "English is a language that is spoken in many countries.",
                  false,
                  List.of()),
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

    @Test
    void shouldHandleMultipleLayoutItems() throws JsonProcessingException {
      NoteRefinementLayout layout = new NoteRefinementLayout();
      layout.setItems(
          List.of(
              new NoteRefinementLayoutItem(
                  "p1", "Point 1: First important aspect.", false, List.of()),
              new NoteRefinementLayoutItem(
                  "p2", "Point 2: Second important aspect.", false, List.of()),
              new NoteRefinementLayoutItem(
                  "p3", "Point 3: Third important aspect.", false, List.of()),
              new NoteRefinementLayoutItem(
                  "p4", "Point 4: Fourth important aspect.", false, List.of()),
              new NoteRefinementLayoutItem(
                  "p5", "Point 5: Fifth important aspect.", false, List.of())));
      openAiStructuredResponseMock.stubStructuredResponse(layout);

      NoteRefinementLayout result = service.generateRefinementSuggestions();

      assertThat(result.getItems(), hasSize(5));
      assertThat(
          result.getItems().stream().map(NoteRefinementLayoutItem::getText).toList(),
          hasItem("Point 1: First important aspect."));
      assertThat(
          result.getItems().stream().map(NoteRefinementLayoutItem::getText).toList(),
          hasItem("Point 5: Fifth important aspect."));
    }

    @Test
    void shouldReturnAllLayoutItemsFromAiResponse() throws JsonProcessingException {
      NoteRefinementLayout layout = new NoteRefinementLayout();
      layout.setItems(
          List.of(
              new NoteRefinementLayoutItem("p1", "Point 1", false, List.of()),
              new NoteRefinementLayoutItem("p2", "Point 2", false, List.of()),
              new NoteRefinementLayoutItem("p3", "Point 3", false, List.of()),
              new NoteRefinementLayoutItem("p4", "Point 4", false, List.of()),
              new NoteRefinementLayoutItem("p5", "Point 5", false, List.of()),
              new NoteRefinementLayoutItem("p6", "Point 6", false, List.of()),
              new NoteRefinementLayoutItem("p7", "Point 7", false, List.of())));
      openAiStructuredResponseMock.stubStructuredResponse(layout);

      NoteRefinementLayout result = service.generateRefinementSuggestions();

      assertThat(result.getItems(), hasSize(7));
    }
  }
}

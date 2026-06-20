package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.NoteRefinementLayoutDTO;
import com.odde.doughnut.controllers.dto.NoteRefinementLayoutSelectionRequestDTO;
import com.odde.doughnut.controllers.dto.RefinedContentResponseDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.NoteRefinementLayout;
import com.odde.doughnut.services.ai.NoteRefinementLayoutItem;
import com.odde.doughnut.services.ai.NoteRefinementLayoutValidator;
import com.odde.doughnut.services.ai.RegeneratedNoteContent;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseTextConfig;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.List;
import java.util.Optional;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

class AiControllerNoteRefinementTest extends ControllerTestBase {
  @Autowired AiController controller;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class GenerateRefinementSuggestions {
    Note testNote;
    OpenAiStructuredResponseMock openAiStructuredResponseMock;

    @BeforeEach
    void setup() {
      testNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldReturnEmptyListWhenNoteContentIsBlank(String content)
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      testNote.setContent(content);

      NoteRefinementLayoutDTO result = controller.generateRefinementSuggestions(testNote);

      assertThat(result.getItems()).isEmpty();
    }

    @Test
    void shouldCallResponsesApiWithStructuredInstructions()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      NoteRefinementLayout layout =
          new NoteRefinementLayout(
              List.of(
                  new NoteRefinementLayoutItem(
                      "p1",
                      "Point 1",
                      false,
                      List.of(
                          new NoteRefinementLayoutItem(
                              "p1-1", "[[Already extracted note]]", true, List.of()))),
                  new NoteRefinementLayoutItem("p2", "Point 2", false, List.of())));
      openAiStructuredResponseMock.stubStructuredResponse(layout);
      testNote.setContent("Some note content");

      NoteRefinementLayoutDTO result = controller.generateRefinementSuggestions(testNote);
      assertThat(result.getItems()).hasSize(2);
      assertThat(result.getItems().getFirst().getText()).isEqualTo("Point 1");
      assertThat(result.getItems().getFirst().getChildren().getFirst().isAlreadyExtracted())
          .isTrue();

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<NoteRefinementLayout>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      StructuredResponseCreateParams<NoteRefinementLayout> params = paramsCaptor.getValue();
      String instructions = params.rawParams().instructions().orElse("");
      MatcherAssert.assertThat(
          "Instructions should request one current-content layout",
          instructions.contains("Return one current-content layout for the note content"),
          Matchers.is(true));
      MatcherAssert.assertThat(
          "Instructions should prohibit alternative breakdown suggestions",
          instructions.contains("not alternative breakdown suggestions"),
          Matchers.is(true));
      MatcherAssert.assertThat(
          "Instructions should prohibit grandchildren",
          instructions.contains("Do not create grandchildren"),
          Matchers.is(true));
      MatcherAssert.assertThat(
          "Instructions should describe wiki-link-only extracted markers",
          instructions.contains("simple standalone wiki-link-only lines"),
          Matchers.is(true));
      MatcherAssert.assertThat(
          "Should use Responses structured text format",
          params.rawParams().text().flatMap(ResponseTextConfig::format).isPresent(),
          Matchers.is(true));
      assertThat(params.rawParams().input().flatMap(input -> input.text()).orElse("")).isNotBlank();
      assertThat(params.rawParams().maxOutputTokens()).isEqualTo(Optional.of(1000L));
    }

    @Test
    void shouldReturnEmptyLayoutWhenAiReturnsInvalidLayout()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
      Logger validatorLogger = loggerContext.getLogger(NoteRefinementLayoutValidator.class);
      Level originalLevel = validatorLogger.getLevel();
      validatorLogger.setLevel(Level.ALL);
      ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
      logAppender.setContext(loggerContext);
      logAppender.start();
      validatorLogger.addAppender(logAppender);
      try {
        NoteRefinementLayout layoutWithDuplicateIds =
            new NoteRefinementLayout(
                List.of(
                    new NoteRefinementLayoutItem("same", "Point 1", false, List.of()),
                    new NoteRefinementLayoutItem("same", "Point 2", false, List.of())));
        openAiStructuredResponseMock.stubStructuredResponse(layoutWithDuplicateIds);
        testNote.setContent("Some note content");

        NoteRefinementLayoutDTO result = controller.generateRefinementSuggestions(testNote);

        assertThat(result.getItems()).isEmpty();
        assertThat(logAppender.list)
            .anyMatch(
                event ->
                    event.getLevel() == Level.WARN
                        && event.getFormattedMessage().contains("duplicate item id"));
      } finally {
        logAppender.stop();
        validatorLogger.detachAppender(logAppender);
        validatorLogger.setLevel(originalLevel);
      }
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class, () -> controller.generateRefinementSuggestions(testNote));
    }
  }

  @Nested
  class RemoveRefinementSuggestion {
    Note testNote;
    OpenAiStructuredResponseMock openAiStructuredResponseMock;

    @BeforeEach
    void setup() {
      testNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
    }

    private NoteRefinementLayout sampleLayout() {
      return new NoteRefinementLayout(
          List.of(
              new NoteRefinementLayoutItem(
                  "p1",
                  "Main concept",
                  false,
                  List.of(
                      new NoteRefinementLayoutItem(
                          "p1-1", "suggestion to remove", false, List.of()))),
              new NoteRefinementLayoutItem("p2", "Other point", false, List.of())));
    }

    private NoteRefinementLayoutSelectionRequestDTO layoutSelectionRequest(
        NoteRefinementLayout layout, List<String> selectedItemIds) {
      NoteRefinementLayoutSelectionRequestDTO requestDTO =
          new NoteRefinementLayoutSelectionRequestDTO();
      requestDTO.setLayout(layout);
      requestDTO.setSelectedItemIds(selectedItemIds);
      return requestDTO;
    }

    @Test
    void shouldReturnRegeneratedContentAfterRemovingSelectedLayoutPoints()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      openAiStructuredResponseMock.stubStructuredResponse(
          new RegeneratedNoteContent("Remaining content."));
      String originalContent = "Original with a suggestion to remove.";
      testNote.setContent(originalContent);
      NoteRefinementLayout layout = sampleLayout();
      RefinedContentResponseDTO response =
          controller.removeRefinementSuggestion(
              testNote, layoutSelectionRequest(layout, List.of("p1-1", "p2")));
      assertThat(response.getContent()).isEqualTo("Remaining content.");
      makeMe.entityPersister.refresh(testNote);
      assertThat(testNote.getContent()).isEqualTo(originalContent);

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<RegeneratedNoteContent>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      StructuredResponseCreateParams<RegeneratedNoteContent> params = paramsCaptor.getValue();
      String instructions = params.rawParams().instructions().orElse("");
      assertThat(params.rawParams().maxOutputTokens()).isEqualTo(Optional.of(2000L));
      assertThat(instructions).contains("Full note layout:");
      assertThat(instructions).contains("\"id\" : \"p1-1\"");
      assertThat(instructions).contains("Selected layout item ids to remove");
      assertThat(instructions).contains("[p1-1, p2]");
      assertThat(instructions).contains("- p1-1: \"suggestion to remove\"");
      assertThat(instructions).contains("- p2: \"Other point\"");
    }

    @Test
    void shouldThrowWhenSelectedItemIdsIsEmpty() {
      testNote.setContent("Some note content.");
      assertBadRequestContaining(
          () ->
              controller.removeRefinementSuggestion(
                  testNote, layoutSelectionRequest(sampleLayout(), List.of())),
          "selectedItemIds cannot be empty");
    }

    @Test
    void shouldThrowWhenNoteContentIsEmpty() {
      testNote.setContent("");
      assertBadRequestContaining(
          () ->
              controller.removeRefinementSuggestion(
                  testNote, layoutSelectionRequest(sampleLayout(), List.of("p1-1"))),
          "Note content cannot be empty");
    }
  }

  private static void assertBadRequestContaining(Executable action, String substring) {
    ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(ex.getReason()).contains(substring);
  }
}

package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.NoteRefinementLayoutDTO;
import com.odde.doughnut.controllers.dto.RefinedContentResponseDTO;
import com.odde.doughnut.controllers.dto.RefinementSuggestionsRequestDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.NoteRefinementLayout;
import com.odde.doughnut.services.ai.NoteRefinementLayoutItem;
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
      NoteRefinementLayout layoutWithDuplicateIds =
          new NoteRefinementLayout(
              List.of(
                  new NoteRefinementLayoutItem("same", "Point 1", false, List.of()),
                  new NoteRefinementLayoutItem("same", "Point 2", false, List.of())));
      openAiStructuredResponseMock.stubStructuredResponse(layoutWithDuplicateIds);
      testNote.setContent("Some note content");

      NoteRefinementLayoutDTO result = controller.generateRefinementSuggestions(testNote);

      assertThat(result.getItems()).isEmpty();
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

    @Test
    void shouldReturnRegeneratedContentAfterRemovingSuggestions()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      openAiStructuredResponseMock.stubStructuredResponse(
          new RegeneratedNoteContent("Remaining content."));
      String originalContent = "Original with a suggestion to remove.";
      testNote.setContent(originalContent);
      RefinementSuggestionsRequestDTO requestDTO = new RefinementSuggestionsRequestDTO();
      requestDTO.suggestions = List.of("suggestion to remove");
      RefinedContentResponseDTO response =
          controller.removeRefinementSuggestion(testNote, requestDTO);
      assertThat(response.getContent()).isEqualTo("Remaining content.");
      makeMe.entityPersister.refresh(testNote);
      assertThat(testNote.getContent()).isEqualTo(originalContent);

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<RegeneratedNoteContent>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      assertThat(paramsCaptor.getValue().rawParams().maxOutputTokens())
          .isEqualTo(Optional.of(2000L));
    }

    @Test
    void shouldThrowWhenSuggestionsToRemoveIsEmpty() {
      testNote.setContent("Some note content.");
      RefinementSuggestionsRequestDTO requestDTO = new RefinementSuggestionsRequestDTO();
      requestDTO.suggestions = List.of();
      assertBadRequestContaining(
          () -> controller.removeRefinementSuggestion(testNote, requestDTO),
          "Suggestions to remove cannot be empty");
    }

    @Test
    void shouldThrowWhenNoteContentIsEmpty() {
      testNote.setContent("");
      RefinementSuggestionsRequestDTO requestDTO = new RefinementSuggestionsRequestDTO();
      requestDTO.suggestions = List.of("some suggestion");
      assertBadRequestContaining(
          () -> controller.removeRefinementSuggestion(testNote, requestDTO),
          "Note content cannot be empty");
    }
  }

  private static void assertBadRequestContaining(Executable action, String substring) {
    ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(ex.getReason()).contains(substring);
  }
}

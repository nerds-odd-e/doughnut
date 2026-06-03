package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.RefinementSuggestionsRequestDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.NoteExtractionResult;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

class AiControllerExtractNoteTest extends ControllerTestBase {
  @Autowired AiController controller;
  @Autowired NoteRepository noteRepository;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class ExtractNote {
    OpenAiStructuredResponseMock openAiStructuredResponseMock;

    @BeforeEach
    void setup() {
      openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
    }

    private Note newRootNoteWithExtractableContent() {
      Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      note.setContent("Original content with a key suggestion to extract.");
      return note;
    }

    private void stubExtractionResult(String newTitle, String newContent, String updatedParent) {
      NoteExtractionResult aiResult = new NoteExtractionResult();
      aiResult.setNewNoteTitle(newTitle);
      aiResult.setNewNoteContent(newContent);
      aiResult.setUpdatedParentContent(updatedParent);
      openAiStructuredResponseMock.stubStructuredResponse(aiResult);
    }

    @Test
    void shouldCallExtractNoteWithStructuredInstructions()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note testNote = newRootNoteWithExtractableContent();
      stubExtractionResult(
          "Extracted Note", "Expanded content for the new note.", "Updated parent with summary.");

      RefinementSuggestionsRequestDTO requestDTO = new RefinementSuggestionsRequestDTO();
      requestDTO.setSuggestions(List.of("key suggestion to extract"));
      controller.extractNote(testNote, requestDTO);

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<NoteExtractionResult>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      StructuredResponseCreateParams<NoteExtractionResult> params = paramsCaptor.getValue();
      String instructions = params.rawParams().instructions().orElse("");
      assertThat(params.rawParams().maxOutputTokens()).isEqualTo(Optional.of(3000L));
      assertThat(instructions).contains("wiki link from the original note");
      assertThat(instructions).contains("Wiki links are case-insensitive");
      assertThat(instructions).contains("[[Canonical Note Title|visible text]]");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldPlaceExtractedNoteAtExpectedLocation(boolean sourceInFolder)
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note sourceNote;
      Folder expectedFolder = null;
      if (sourceInFolder) {
        Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
        expectedFolder = makeMe.aFolder().notebook(notebook).name("Context").please();
        sourceNote =
            makeMe
                .aNote()
                .title("Sample")
                .folder(expectedFolder)
                .content("Original content with a key suggestion to extract.")
                .please();
      } else {
        sourceNote = newRootNoteWithExtractableContent();
      }

      stubExtractionResult(
          sourceInFolder ? "Point B" : "Extracted Note",
          sourceInFolder ? "Extracted" : "Expanded content for the new note.",
          sourceInFolder ? "A. C. D. E." : "Updated parent with summary.");

      RefinementSuggestionsRequestDTO requestDTO = new RefinementSuggestionsRequestDTO();
      requestDTO.setSuggestions(List.of("key suggestion to extract"));
      NoteRealm response = controller.extractNote(sourceNote, requestDTO);

      Note persistedNote = noteRepository.findById(response.getNote().getId()).orElseThrow();
      if (sourceInFolder) {
        assertThat(persistedNote.getFolder().getId()).isEqualTo(expectedFolder.getId());
      } else {
        assertThat(persistedNote.getFolder()).isNull();
        assertThat(noteRepository.findById(sourceNote.getId()).orElseThrow().getContent())
            .isEqualTo("Updated parent with summary.");
      }
    }

    @Test
    void shouldRefreshWikiLinkCacheForOriginalAndNewNoteAfterExtraction()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note testNote =
          makeMe
              .aNote()
              .title("Sample")
              .notebookOwnedBy(currentUser.getUser())
              .content("A. B. C.")
              .please();
      stubExtractionResult(
          "Point B",
          "Extracted from [[sample|the original note]].",
          "A. See [[point b|the extracted note]]. C.");

      RefinementSuggestionsRequestDTO requestDTO = new RefinementSuggestionsRequestDTO();
      requestDTO.setSuggestions(List.of("B"));
      NoteRealm response = controller.extractNote(testNote, requestDTO);

      assertThat(response.getWikiTitles())
          .anyMatch(
              wikiTitle ->
                  wikiTitle.getTargetToken().equals("sample")
                      && wikiTitle.getDisplayText().equals("the original note")
                      && wikiTitle.getNoteId().equals(testNote.getId()));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      Note testNote = newRootNoteWithExtractableContent();
      currentUser.setUser(null);
      RefinementSuggestionsRequestDTO requestDTO = new RefinementSuggestionsRequestDTO();
      requestDTO.setSuggestions(List.of("a suggestion"));
      assertThrows(
          ResponseStatusException.class, () -> controller.extractNote(testNote, requestDTO));
    }

    static Stream<List<String>> invalidExtractNoteSuggestionLists() {
      return Stream.of(null, List.of(), List.of("a", "b"));
    }

    @ParameterizedTest
    @MethodSource("invalidExtractNoteSuggestionLists")
    void shouldRejectInvalidSuggestionCount(List<String> suggestions) {
      Note testNote = newRootNoteWithExtractableContent();
      RefinementSuggestionsRequestDTO requestDTO = new RefinementSuggestionsRequestDTO();
      requestDTO.setSuggestions(suggestions);
      assertResponseStatus(
          () -> controller.extractNote(testNote, requestDTO), HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldThrowWhenAiReturnsNull() {
      Note testNote = newRootNoteWithExtractableContent();
      openAiStructuredResponseMock.stubStructuredResponse(null);
      RefinementSuggestionsRequestDTO requestDTO = new RefinementSuggestionsRequestDTO();
      requestDTO.setSuggestions(List.of("a suggestion"));
      assertResponseStatus(
          () -> controller.extractNote(testNote, requestDTO), HttpStatus.SERVICE_UNAVAILABLE);
    }
  }

  private static void assertResponseStatus(Executable action, HttpStatus expected) {
    assertThat(assertThrows(ResponseStatusException.class, action).getStatusCode())
        .isEqualTo(expected);
  }
}

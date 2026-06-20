package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteRefinementExtractRequestDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.NoteExtractionResult;
import com.odde.doughnut.services.ai.NoteRefinementLayout;
import com.odde.doughnut.services.ai.NoteRefinementLayoutItem;
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

    private NoteRefinementLayout sampleLayout() {
      return new NoteRefinementLayout(
          List.of(
              new NoteRefinementLayoutItem(
                  "p1",
                  "Main concept",
                  false,
                  List.of(
                      new NoteRefinementLayoutItem(
                          "p1-1", "key suggestion to extract", false, List.of()))),
              new NoteRefinementLayoutItem("p2", "Other point", false, List.of())));
    }

    private NoteRefinementLayout layoutWithItem(String id, String text) {
      return new NoteRefinementLayout(
          List.of(new NoteRefinementLayoutItem(id, text, false, List.of())));
    }

    private NoteRefinementExtractRequestDTO extractRequest(
        NoteRefinementLayout layout, List<String> selectedItemIds) {
      NoteRefinementExtractRequestDTO requestDTO = new NoteRefinementExtractRequestDTO();
      requestDTO.setLayout(layout);
      requestDTO.setSelectedItemIds(selectedItemIds);
      return requestDTO;
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
      NoteRefinementLayout layout = sampleLayout();

      controller.extractNote(testNote, extractRequest(layout, List.of("p1-1", "p2")));

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<NoteExtractionResult>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      StructuredResponseCreateParams<NoteExtractionResult> params = paramsCaptor.getValue();
      String instructions = params.rawParams().instructions().orElse("");
      assertThat(params.rawParams().maxOutputTokens()).isEqualTo(Optional.of(3000L));
      assertThat(instructions).contains("Full note layout:");
      assertThat(instructions).contains("\"id\" : \"p1-1\"");
      assertThat(instructions).contains("Selected layout item ids to extract together");
      assertThat(instructions).contains("[p1-1, p2]");
      assertThat(instructions).contains("- p1-1: \"key suggestion to extract\"");
      assertThat(instructions).contains("- p2: \"Other point\"");
      assertThat(instructions).contains("wiki link from the original note");
      assertThat(instructions).contains("Wiki links are case-insensitive");
      assertThat(instructions).contains("[[Canonical Note Title|visible text]]");
      assertThat(instructions).contains("alreadyExtracted");
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

      NoteRefinementLayout layout = layoutWithItem("p1", "key suggestion to extract");
      NoteRealm response =
          controller.extractNote(sourceNote, extractRequest(layout, List.of("p1")));
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
    void shouldSanitizePathSeparatorsInExtractedNoteTitleAndWikiLinks()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note testNote = newRootNoteWithExtractableContent();
      stubExtractionResult(
          "foo/bar: baz",
          "See [[foo/bar: baz|link]] and [[MyNb:foo/bar|nb]].",
          "Back to [[foo/bar: baz]].");
      NoteRefinementLayout layout = layoutWithItem("p1", "key suggestion to extract");

      NoteRealm response = controller.extractNote(testNote, extractRequest(layout, List.of("p1")));

      Note persistedNote = noteRepository.findById(response.getNote().getId()).orElseThrow();
      assertThat(persistedNote.getTitle()).isEqualTo("foo／bar： baz");
      assertThat(persistedNote.getContent())
          .isEqualTo("See [[foo／bar： baz|link]] and [[MyNb:foo／bar|nb]].");
      assertThat(noteRepository.findById(testNote.getId()).orElseThrow().getContent())
          .isEqualTo("Back to [[foo／bar： baz]].");
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
      NoteRefinementLayout layout = layoutWithItem("p1", "B");

      NoteRealm response = controller.extractNote(testNote, extractRequest(layout, List.of("p1")));

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
      NoteRefinementLayout layout = layoutWithItem("p1", "a suggestion");
      assertThrows(
          ResponseStatusException.class,
          () -> controller.extractNote(testNote, extractRequest(layout, List.of("p1"))));
    }

    static Stream<List<String>> invalidSelectedItemIds() {
      return Stream.of(null, List.of(), List.of("missing-id"));
    }

    @ParameterizedTest
    @MethodSource("invalidSelectedItemIds")
    void shouldRejectInvalidSelectedItemIds(List<String> selectedItemIds) {
      Note testNote = newRootNoteWithExtractableContent();
      NoteRefinementLayout layout = layoutWithItem("p1", "a suggestion");
      assertResponseStatus(
          () -> controller.extractNote(testNote, extractRequest(layout, selectedItemIds)),
          HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectInvalidLayout() {
      Note testNote = newRootNoteWithExtractableContent();
      NoteRefinementLayout layout =
          new NoteRefinementLayout(
              List.of(new NoteRefinementLayoutItem("", "a suggestion", false, List.of())));
      assertResponseStatus(
          () -> controller.extractNote(testNote, extractRequest(layout, List.of("p1"))),
          HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldThrowWhenAiReturnsNull() {
      Note testNote = newRootNoteWithExtractableContent();
      openAiStructuredResponseMock.stubStructuredResponse(null);
      NoteRefinementLayout layout = layoutWithItem("p1", "a suggestion");
      assertResponseStatus(
          () -> controller.extractNote(testNote, extractRequest(layout, List.of("p1"))),
          HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void shouldRejectBlankNoteContent() {
      Note testNote = newRootNoteWithExtractableContent();
      testNote.setContent("");
      NoteRefinementLayout layout = layoutWithItem("p1", "a suggestion");
      assertResponseStatus(
          () -> controller.extractNote(testNote, extractRequest(layout, List.of("p1"))),
          HttpStatus.BAD_REQUEST);
    }
  }

  private static void assertResponseStatus(Executable action, HttpStatus expected) {
    assertThat(assertThrows(ResponseStatusException.class, action).getStatusCode())
        .isEqualTo(expected);
  }
}

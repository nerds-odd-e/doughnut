package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.PointsRequestDTO;
import com.odde.doughnut.controllers.dto.RemovePointsResponseDTO;
import com.odde.doughnut.controllers.dto.SuggestedTitleDTO;
import com.odde.doughnut.controllers.dto.UnderstandingChecklistDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.PointExtractionResult;
import com.odde.doughnut.services.ai.RegeneratedNoteContent;
import com.odde.doughnut.services.ai.TitleReplacement;
import com.odde.doughnut.services.ai.UnderstandingChecklist;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.openai.client.OpenAIClient;
import com.openai.models.models.ModelListPage;
import com.openai.services.blocking.ModelService;
import java.util.List;
import java.util.stream.Stream;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

class AiControllerTest extends ControllerTestBase {
  @Autowired AiController controller;
  @Autowired NoteRepository noteRepository;

  Note note;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @BeforeEach
  void Setup() {
    currentUser.setUser(makeMe.aUser().please());
    note = makeMe.aNote().please();
  }

  @Nested
  class GetModelVersions {

    @Test
    void shouldGetModelVersionsCorrectly() {
      // Mock the official SDK models API
      ModelService modelService = Mockito.mock(ModelService.class);
      when(officialClient.models()).thenReturn(modelService);

      com.openai.models.models.Model officialModel1 =
          com.openai.models.models.Model.builder()
              .id("gpt-4")
              .created(System.currentTimeMillis() / 1000)
              .ownedBy("openai")
              .build();
      com.openai.models.models.Model officialModel2 =
          com.openai.models.models.Model.builder()
              .id("any-model")
              .created(System.currentTimeMillis() / 1000)
              .ownedBy("openai")
              .build();

      // Mock the models list response
      // officialClient.models() returns ModelService, list() returns ModelListPage, data() returns
      // List<Model>
      var modelsList = List.of(officialModel1, officialModel2);
      ModelListPage mockListPage = Mockito.mock(ModelListPage.class);
      when(modelService.list()).thenReturn(mockListPage);
      when(mockListPage.data()).thenReturn(modelsList);

      assertThat(controller.getAvailableGptModels()).contains("gpt-4");
    }

    @Test
    void shouldThrowWhenOpenAiNotAvailable() {
      testabilitySettings.setOpenAiTokenOverride("");
      assertThrows(OpenAiNotAvailableException.class, () -> controller.getAvailableGptModels());
    }
  }

  @Nested
  class SuggestNoteTitle {
    Note testNote;
    OpenAIChatCompletionMock openAIChatCompletionMock;

    @BeforeEach
    void setup() {
      testNote = makeMe.aNote().notebookCreatorAndOwner(currentUser.getUser()).please();
      openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);
      TitleReplacement suggestedTopic = new TitleReplacement();
      suggestedTopic.setNewTitle("Suggested Title");
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(suggestedTopic);
    }

    @Test
    void shouldReturnSuggestedTitle()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      SuggestedTitleDTO result = controller.suggestTitle(testNote);
      assertThat(result.getTitle()).isEqualTo("Suggested Title");
    }

    @Test
    void shouldCallChatCompletionWithRightMessage()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      controller.suggestTitle(testNote);
      ArgumentCaptor<com.openai.models.chat.completions.ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(
              com.openai.models.chat.completions.ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService()).create(paramsCaptor.capture());
      com.openai.models.chat.completions.ChatCompletionCreateParams params =
          paramsCaptor.getValue();
      boolean hasInstruction =
          params.messages().stream()
              .map(Object::toString)
              .anyMatch(msg -> msg.contains("Please suggest a better title for the note"));
      MatcherAssert.assertThat(
          "A message should contain the instruction", hasInstruction, is(true));
      MatcherAssert.assertThat(
          "Should use responseFormat instead of tools",
          params.responseFormat().isPresent(),
          is(true));
      MatcherAssert.assertThat(
          "Should not have tools",
          params.tools().map(list -> list.isEmpty()).orElse(true),
          is(true));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.suggestTitle(testNote));
    }
  }

  @Nested
  class GenerateUnderstandingChecklist {
    Note testNote;
    OpenAIChatCompletionMock openAIChatCompletionMock;

    @BeforeEach
    void setup() {
      testNote = makeMe.aNote().notebookCreatorAndOwner(currentUser.getUser()).please();
      openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);
    }

    @Test
    void shouldReturnUnderstandingPoints()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      UnderstandingChecklist understandingChecklist = new UnderstandingChecklist();
      understandingChecklist.setPoints(
          List.of(
              "English is a language that is spoken in many countries.",
              "It is also the most widely spoken language in the world."));
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(understandingChecklist);
      testNote.setContent("English is a language that is spoken in many countries.");

      UnderstandingChecklistDTO result = controller.generateUnderstandingChecklist(testNote);

      assertThat(result.getPoints())
          .containsExactly(
              "English is a language that is spoken in many countries.",
              "It is also the most widely spoken language in the world.");
    }

    @Test
    void shouldReturnEmptyListWhenNoteContentIsNull()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      testNote.setContent(null);

      UnderstandingChecklistDTO result = controller.generateUnderstandingChecklist(testNote);

      assertThat(result.getPoints()).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenNoteContentIsEmpty()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      testNote.setContent("");

      UnderstandingChecklistDTO result = controller.generateUnderstandingChecklist(testNote);

      assertThat(result.getPoints()).isEmpty();
    }

    @Test
    void shouldReturnEmptyListWhenNoteContentIsWhitespace()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      testNote.setContent("   ");

      UnderstandingChecklistDTO result = controller.generateUnderstandingChecklist(testNote);

      assertThat(result.getPoints()).isEmpty();
    }

    @Test
    void shouldCallChatCompletionWithRightMessage()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      UnderstandingChecklist understandingChecklist = new UnderstandingChecklist();
      understandingChecklist.setPoints(List.of("Point 1", "Point 2"));
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(understandingChecklist);
      testNote.setContent("Some note content");

      controller.generateUnderstandingChecklist(testNote);

      ArgumentCaptor<com.openai.models.chat.completions.ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(
              com.openai.models.chat.completions.ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService()).create(paramsCaptor.capture());
      com.openai.models.chat.completions.ChatCompletionCreateParams params =
          paramsCaptor.getValue();
      boolean hasInstruction =
          params.messages().stream()
              .map(Object::toString)
              .anyMatch(
                  msg ->
                      msg.contains(
                          "Please generate an understanding checklist of the note content broken down into key points"));
      MatcherAssert.assertThat(
          "A message should contain the instruction", hasInstruction, is(true));
      MatcherAssert.assertThat(
          "Should use responseFormat instead of tools",
          params.responseFormat().isPresent(),
          is(true));
      MatcherAssert.assertThat(
          "Should not have tools",
          params.tools().map(list -> list.isEmpty()).orElse(true),
          is(true));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class, () -> controller.generateUnderstandingChecklist(testNote));
    }
  }

  @Nested
  class RemovePointFromNote {
    Note testNote;
    RemovePointsResponseDTO result;
    OpenAIChatCompletionMock openAIChatCompletionMock;

    @BeforeEach
    void setup() {
      testNote = makeMe.aNote().notebookCreatorAndOwner(currentUser.getUser()).please();
      openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);
    }

    @Test
    void shouldReturnRegeneratedContentAfterRemovingPoints()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(
          new RegeneratedNoteContent("Remaining content."));
      String originalContent =
          "English is a language that is spoken in many countries. It is also the most widely spoken language in the world.";
      testNote.setContent(originalContent);
      PointsRequestDTO requestDTO = new PointsRequestDTO();
      requestDTO.points = List.of("English is a language that is spoken in many countries.");
      RemovePointsResponseDTO response = controller.removePointFromNote(testNote, requestDTO);
      assertThat(response.getContent()).isNotEqualTo(originalContent);
      assertThat(response.getContent()).isEqualTo("Remaining content.");
    }

    @Test
    void shouldNotModifyNoteInDatabase()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(
          new RegeneratedNoteContent("Remaining content."));
      String originalContent = "Original content with point to remove.";
      testNote.setContent(originalContent);
      PointsRequestDTO requestDTO = new PointsRequestDTO();
      requestDTO.points = List.of("point to remove");
      controller.removePointFromNote(testNote, requestDTO);
      makeMe.entityPersister.flush();
      makeMe.entityPersister.refresh(testNote);
      assertThat(testNote.getContent()).isEqualTo(originalContent);
    }

    @Test
    void shouldThrowWhenPointsToRemoveIsEmpty() {
      testNote.setContent("Some note content.");
      PointsRequestDTO requestDTO = new PointsRequestDTO();
      requestDTO.points = List.of();
      assertBadRequestContaining(
          () -> controller.removePointFromNote(testNote, requestDTO),
          "Points to remove cannot be empty");
    }

    @Test
    void shouldThrowWhenNoteContentIsEmpty() {
      testNote.setContent("");
      PointsRequestDTO requestDTO = new PointsRequestDTO();
      requestDTO.points = List.of("some point");
      assertBadRequestContaining(
          () -> controller.removePointFromNote(testNote, requestDTO),
          "Note content cannot be empty");
    }
  }

  @Nested
  class PromotePointToSibling {
    OpenAIChatCompletionMock openAIChatCompletionMock;

    @BeforeEach
    void setup() {
      openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);
    }

    private Note newRootNoteWithPromotableContent() {
      Note note = makeMe.aNote().notebookCreatorAndOwner(currentUser.getUser()).please();
      note.setContent("Original content with a key point to promote.");
      return note;
    }

    @Test
    void shouldReturnCreatedSiblingNoteAtNotebookRootWhenSourceNoteHasNoFolder()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note testNote = newRootNoteWithPromotableContent();
      PointExtractionResult aiResult = new PointExtractionResult();
      aiResult.setNewNoteTitle("Extracted Sibling Note");
      aiResult.setNewNoteContent("Expanded content for the sibling.");
      aiResult.setUpdatedParentContent("Updated parent with summary.");
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(aiResult);

      PointsRequestDTO requestDTO = new PointsRequestDTO();
      requestDTO.setPoints(List.of("key point to promote"));
      NoteRealm response = controller.promotePointToSibling(testNote, requestDTO);

      assertThat(response.getNote().getTitle()).isEqualTo("Extracted Sibling Note");
      assertThat(response.getNote().getContent()).isEqualTo("Expanded content for the sibling.");
      assertThat(noteRepository.findById(testNote.getId()).orElseThrow().getContent())
          .isEqualTo("Updated parent with summary.");
      Note sibling = noteRepository.findById(response.getNote().getId()).orElseThrow();
      assertThat(sibling.getFolder()).isNull();
    }

    @Test
    void shouldPlaceSiblingInSameFolderWhenSourceNoteIsInFolder()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Notebook notebook = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
      Folder folder = makeMe.aFolder().notebook(notebook).name("Context").please();
      Note noteInFolder =
          makeMe
              .aNote()
              .title("Sample")
              .inNotebook(notebook)
              .folder(folder)
              .content("Original content with a key point to promote.")
              .please();

      PointExtractionResult aiResult = new PointExtractionResult();
      aiResult.setNewNoteTitle("Point B");
      aiResult.setNewNoteContent("Extracted");
      aiResult.setUpdatedParentContent("A. C. D. E.");
      openAIChatCompletionMock.mockChatCompletionAndReturnJsonSchema(aiResult);

      PointsRequestDTO requestDTO = new PointsRequestDTO();
      requestDTO.setPoints(List.of("key point to promote"));
      NoteRealm response = controller.promotePointToSibling(noteInFolder, requestDTO);

      assertThat(response.getNote().getTitle()).isEqualTo("Point B");
      Note persistedSibling = noteRepository.findById(response.getNote().getId()).orElseThrow();
      assertThat(persistedSibling.getFolder().getId()).isEqualTo(folder.getId());
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      Note testNote = newRootNoteWithPromotableContent();
      currentUser.setUser(null);
      PointsRequestDTO requestDTO = new PointsRequestDTO();
      requestDTO.setPoints(List.of("a point"));
      assertThrows(
          ResponseStatusException.class,
          () -> controller.promotePointToSibling(testNote, requestDTO));
    }

    static Stream<List<String>> invalidPromotePointLists() {
      return Stream.of(null, List.of(), List.of("a", "b"));
    }

    @ParameterizedTest
    @MethodSource("invalidPromotePointLists")
    void shouldRejectInvalidPointCount(List<String> points) {
      Note testNote = newRootNoteWithPromotableContent();
      PointsRequestDTO requestDTO = new PointsRequestDTO();
      requestDTO.setPoints(points);
      assertResponseStatus(
          () -> controller.promotePointToSibling(testNote, requestDTO), HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldThrowWhenAiReturnsNull()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note testNote = newRootNoteWithPromotableContent();
      openAIChatCompletionMock.mockNullChatCompletion();
      PointsRequestDTO requestDTO = new PointsRequestDTO();
      requestDTO.setPoints(List.of("a point"));
      assertResponseStatus(
          () -> controller.promotePointToSibling(testNote, requestDTO),
          HttpStatus.SERVICE_UNAVAILABLE);
    }
  }

  private static void assertResponseStatus(Executable action, HttpStatus expected) {
    assertThat(assertThrows(ResponseStatusException.class, action).getStatusCode())
        .isEqualTo(expected);
  }

  private static void assertBadRequestContaining(Executable action, String substring) {
    ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(ex.getReason()).contains(substring);
  }
}

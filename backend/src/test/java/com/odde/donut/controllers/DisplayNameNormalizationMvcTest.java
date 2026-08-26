package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.FolderRepository;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.services.EmbeddingService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@AutoConfigureMockMvc
class DisplayNameNormalizationMvcTest extends ControllerTestBase {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private NoteRepository noteRepository;
  @Autowired private FolderRepository folderRepository;
  @Autowired private NotebookRepository notebookRepository;

  @MockitoBean private EmbeddingService embeddingService;

  @BeforeEach
  void setup() {
    when(embeddingService.streamEmbeddingsForNoteList(any()))
        .thenAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              List<Note> notes = (List<Note>) invocation.getArgument(0);
              return notes.stream()
                  .map(
                      n ->
                          new EmbeddingService.EmbeddingForNote(
                              n, Optional.of(List.of(1.0f, 2.0f, 3.0f))));
            });
    currentUser.setUser(makeMe.aUser().please());
  }

  @Test
  void createExtractedNoteStoresTitleWithoutSurroundingWhitespace() throws Exception {
    Note source =
        makeMe
            .aNote()
            .notebookOwnedBy(currentUser.getUser())
            .content("Content to extract.")
            .please();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/ai/create-extracted-note/{note}", source.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"newNoteTitle\":\"  Extracted  \",\"newNoteContent\":\"Extracted"
                            + " body.\",\"updatedOriginalNoteContent\":\"Remaining content.\"}"))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    Note created = noteRepository.findById(root.get("note").get("id").asInt()).orElseThrow();
    assertThat(created.getTitle(), equalTo("Extracted"));
  }

  @Test
  void createNoteStoresTitleWithoutSurroundingUnicodeWhitespace() throws Exception {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/notebooks/{notebookId}/create-note", nb.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"newTitle\":\"\\u3000 spaced \\u3000\"}"))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    Note created = noteRepository.findById(root.get("id").asInt()).orElseThrow();
    assertThat(created.getTitle(), equalTo("spaced"));
  }

  @Test
  void updateNoteTitleStoresTitleWithoutSurroundingWhitespace() throws Exception {
    Note note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Before").please();

    mockMvc
        .perform(
            patch("/api/text_content/{note}/title", note.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newTitle\":\"  After  \"}"))
        .andExpect(status().isOk());

    makeMe.refresh(note);
    assertThat(note.getTitle(), equalTo("After"));
  }

  @Test
  void createFolderStoresNameWithoutSurroundingWhitespace() throws Exception {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();

    MvcResult result =
        mockMvc
            .perform(
                post("/api/notebooks/{notebookId}/folders", nb.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"  Inbox  \"}"))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    Folder created = folderRepository.findById(root.get("id").asInt()).orElseThrow();
    assertThat(created.getName(), equalTo("Inbox"));
  }

  @Test
  void renameFolderStoresNameWithoutSurroundingWhitespace() throws Exception {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
    Folder folder = makeMe.aFolder().notebook(nb).name("Old").please();

    mockMvc
        .perform(
            patch("/api/notebooks/{notebookId}/folders/{folderId}", nb.getId(), folder.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  New  \"}"))
        .andExpect(status().isOk());

    makeMe.refresh(folder);
    assertThat(folder.getName(), equalTo("New"));
  }

  @Test
  void createNotebookStoresNameWithoutSurroundingWhitespace() throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/notebooks/create")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"newTitle\":\"  My NB  \"}"))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
    Notebook created =
        notebookRepository.findById(root.get("notebook").get("id").asInt()).orElseThrow();
    assertThat(created.getName(), equalTo("My NB"));
  }

  @Test
  void renameNotebookStoresNameWithoutSurroundingWhitespace() throws Exception {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();

    mockMvc
        .perform(
            post("/api/notebooks/{notebookId}", nb.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"  Renamed  \",\"notebookSettings\":{}}"))
        .andExpect(status().isOk());

    makeMe.refresh(nb);
    assertThat(nb.getName(), equalTo("Renamed"));
  }

  @Test
  void createNoteRejectsUnicodeWhitespaceOnlyTitle() throws Exception {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();

    mockMvc
        .perform(
            post("/api/notebooks/{notebookId}/create-note", nb.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newTitle\":\"\\u3000\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorType").value("BINDING_ERROR"));
  }

  @Test
  void createFolderRejectsUnicodeWhitespaceOnlyName() throws Exception {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();

    mockMvc
        .perform(
            post("/api/notebooks/{notebookId}/folders", nb.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\\u3000\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorType").value("BINDING_ERROR"));
  }

  @Test
  void createNotebookRejectsUnicodeWhitespaceOnlyTitle() throws Exception {
    mockMvc
        .perform(
            post("/api/notebooks/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"newTitle\":\"\\u3000\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorType").value("BINDING_ERROR"));
  }

  @Test
  void renameNotebookRejectsUnicodeWhitespaceOnlyName() throws Exception {
    Notebook nb = makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();

    mockMvc
        .perform(
            post("/api/notebooks/{notebookId}", nb.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\\u3000\",\"notebookSettings\":{}}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorType").value("BINDING_ERROR"));
  }
}

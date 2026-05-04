package com.odde.doughnut.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.EmbeddingService;
import com.odde.doughnut.services.NoteService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class SoftDeletedTitleConflictMvcTest extends ControllerTestBase {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private NoteService noteService;
  @Autowired private NoteRepository noteRepository;

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
  void createNoteReturns409WhenSoftDeletedNoteHasSameTitleAtRoot() throws Exception {
    User owner = currentUser.getUser();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note n = makeMe.aNote().creatorAndOwner(owner).inNotebook(nb).title("DupTitle").please();
    noteService.destroy(n);

    NoteCreationDTO dto = new NoteCreationDTO();
    dto.setNewTitle("DupTitle");

    mockMvc
        .perform(
            post("/api/notebooks/{notebookId}/create-note", nb.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorType").value("SOFT_DELETED_TITLE_CONFLICT"))
        .andExpect(jsonPath("$.errors.deletedNoteId").value(String.valueOf(n.getId())));
  }

  @Test
  void undoDeleteRestoresNoteAfterSoftDeletedTitleConflict() throws Exception {
    User owner = currentUser.getUser();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
    Note n = makeMe.aNote().creatorAndOwner(owner).inNotebook(nb).title("RestoreMe").please();
    noteService.destroy(n);

    NoteCreationDTO dto = new NoteCreationDTO();
    dto.setNewTitle("RestoreMe");
    mockMvc
        .perform(
            post("/api/notebooks/{notebookId}/create-note", nb.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isConflict());

    mockMvc.perform(patch("/api/notes/{noteId}/undo-delete", n.getId())).andExpect(status().isOk());

    Note reloaded = noteRepository.findById(n.getId()).orElseThrow();
    org.junit.jupiter.api.Assertions.assertNull(reloaded.getDeletedAt());
  }

  @Test
  void createNoteReturns409WhenSoftDeletedNoteHasSameTitleInFolder() throws Exception {
    User owner = currentUser.getUser();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder folder = makeMe.aFolder().notebook(nb).name("Box").please();
    Note n =
        makeMe
            .aNote()
            .creatorAndOwner(owner)
            .inNotebook(nb)
            .folder(folder)
            .title("InFolder")
            .please();
    noteService.destroy(n);

    NoteCreationDTO dto = new NoteCreationDTO();
    dto.setNewTitle("InFolder");
    dto.setFolderId(folder.getId());

    mockMvc
        .perform(
            post("/api/notebooks/{notebookId}/create-note", nb.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorType").value("SOFT_DELETED_TITLE_CONFLICT"));
  }
}

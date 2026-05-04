package com.odde.doughnut.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.NoteCreationDTO;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.EmbeddingService;
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
class DuplicateNoteTitleMvcTest extends ControllerTestBase {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

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
  void createNoteAtNotebookRootReturns409WhenTitleDuplicatesAtRoot() throws Exception {
    User owner = currentUser.getUser();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
    makeMe.aNote().creatorAndOwner(owner).inNotebook(nb).title("SameTitle").please();

    NoteCreationDTO dto = new NoteCreationDTO();
    dto.setNewTitle("SameTitle");

    mockMvc
        .perform(
            post("/api/notebooks/{notebookId}/create-note", nb.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorType").value("RESOURCE_CONFLICT"))
        .andExpect(
            jsonPath("$.message")
                .value(containsString("A note with this title already exists in this notebook")));
  }

  @Test
  void createNoteAtNotebookRootReturns409WhenTitleDuplicatesInSameFolder() throws Exception {
    User owner = currentUser.getUser();
    Notebook nb = makeMe.aNotebook().creatorAndOwner(owner).please();
    Folder folder = makeMe.aFolder().notebook(nb).name("F").please();
    makeMe.aNote().creatorAndOwner(owner).inNotebook(nb).folder(folder).title("InFolder").please();

    NoteCreationDTO dto = new NoteCreationDTO();
    dto.setNewTitle("InFolder");
    dto.setFolderId(folder.getId());

    mockMvc
        .perform(
            post("/api/notebooks/{notebookId}/create-note", nb.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorType").value("RESOURCE_CONFLICT"));
  }
}

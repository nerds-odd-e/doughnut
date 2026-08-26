package com.odde.donut.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.dto.NoteCreationDTO;
import com.odde.donut.controllers.dto.NoteDeleteReferenceHandling;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.services.EmbeddingService;
import com.odde.donut.services.NoteService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@AutoConfigureMockMvc
class SoftDeletedTitleConflictMvcTest extends ControllerTestBase {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private NoteService noteService;

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

  private void softDelete(Note note) {
    noteService.destroy(note, NoteDeleteReferenceHandling.LEAVE_DEAD_LINKS, currentUser.getUser());
  }

  private Notebook ownedNotebook() {
    return makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
  }

  private ResultActions expectSoftDeletedTitleConflict(ResultActions result) throws Exception {
    return result
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.errorType").value("SOFT_DELETED_TITLE_CONFLICT"));
  }

  @Test
  void createNoteReturns409WhenSoftDeletedNoteHasSameTitleWithUnicodeSurroundingWhitespace()
      throws Exception {
    Note n = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("A").please();
    softDelete(n);

    NoteCreationDTO dto = new NoteCreationDTO();
    dto.setNewTitle("\u3000 A\u3000");

    expectSoftDeletedTitleConflict(
        mockMvc.perform(
            post("/api/notebooks/{notebookId}/create-note", n.getNotebook().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))));
  }

  @Test
  void createNoteReturns409WhenSoftDeletedNoteHasSameTitleAtRoot() throws Exception {
    Note n = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("DupTitle").please();
    softDelete(n);

    NoteCreationDTO dto = new NoteCreationDTO();
    dto.setNewTitle("DupTitle");

    expectSoftDeletedTitleConflict(
            mockMvc.perform(
                post("/api/notebooks/{notebookId}/create-note", n.getNotebook().getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(dto))))
        .andExpect(jsonPath("$.errors.deletedNoteId").value(String.valueOf(n.getId())));
  }

  @Test
  void createNoteReturns409WhenSoftDeletedNoteHasSameTitleInFolder() throws Exception {
    Notebook nb = ownedNotebook();
    Folder folder = makeMe.aFolder().notebook(nb).name("Box").please();
    Note n = makeMe.aNote().folder(folder).title("InFolder").please();
    softDelete(n);

    NoteCreationDTO dto = new NoteCreationDTO();
    dto.setNewTitle("InFolder");
    dto.setFolderId(folder.getId());

    expectSoftDeletedTitleConflict(
        mockMvc.perform(
            post("/api/notebooks/{notebookId}/create-note", nb.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto))));
  }

  @Test
  void moveNoteToFolderReturns409WhenSoftDeletedNoteHasSameTitleAtDestination() throws Exception {
    Notebook nb = ownedNotebook();
    Folder folderA = makeMe.aFolder().notebook(nb).name("A").please();
    Folder folderB = makeMe.aFolder().notebook(nb).name("B").please();
    Note deleted = makeMe.aNote().folder(folderB).title("DupTitle").please();
    softDelete(deleted);
    Note mover = makeMe.aNote().folder(folderA).title("DupTitle").please();

    expectSoftDeletedTitleConflict(
        mockMvc.perform(
            post(
                "/api/relations/move-to-folder/{sourceNote}/{targetFolder}",
                mover.getId(),
                folderB.getId())));
  }

  @Test
  void moveNoteToNotebookRootReturns409WhenSoftDeletedNoteHasSameTitleAtRoot() throws Exception {
    Notebook nb = ownedNotebook();
    Folder folder = makeMe.aFolder().notebook(nb).name("Box").please();
    Note deleted = makeMe.aNote().notebook(nb).title("DupTitle").please();
    softDelete(deleted);
    Note mover = makeMe.aNote().folder(folder).title("DupTitle").please();

    expectSoftDeletedTitleConflict(
        mockMvc.perform(post("/api/relations/move-to-notebook-root/{sourceNote}", mover.getId())));
  }

  @Test
  void dissolveFolderReturns409WhenSoftDeletedNoteHasSameTitleAtDestination() throws Exception {
    Notebook nb = ownedNotebook();
    Folder outer = makeMe.aFolder().notebook(nb).name("Outer").please();
    Folder mid = makeMe.aFolder().parentFolder(outer).name("Mid").please();
    Note deleted = makeMe.aNote().folder(outer).title("Loose").please();
    softDelete(deleted);
    makeMe.aNote().folder(mid).title("Loose").please();

    expectSoftDeletedTitleConflict(
        mockMvc.perform(
            delete("/api/notebooks/{notebook}/folders/{folder}", nb.getId(), mid.getId())));
  }
}

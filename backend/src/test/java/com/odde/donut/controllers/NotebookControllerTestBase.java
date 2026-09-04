package com.odde.donut.controllers;

import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.controllers.dto.NotebookCatalogGroupItem;
import com.odde.donut.controllers.dto.NotebookCatalogItem;
import com.odde.donut.controllers.dto.NotebookCatalogNotebookItem;
import com.odde.donut.controllers.dto.NotebookCatalogSubscribedNotebookItem;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookSettings;
import com.odde.donut.entities.repositories.NoteRepository;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.entities.repositories.NotebookRepository;
import com.odde.donut.services.EmbeddingService;
import com.odde.donut.services.NoteService;
import com.odde.donut.services.NotebookGroupService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

abstract class NotebookControllerTestBase extends ControllerTestBase {

  @Autowired com.odde.donut.entities.repositories.BazaarNotebookRepository bazaarNotebookRepository;

  @Autowired NotebookController controller;
  @Autowired NoteRepository noteRepository;
  @Autowired NotebookRepository notebookRepository;
  @Autowired NotebookGitBindingRepository notebookGitBindingRepository;
  @Autowired NoteService noteService;
  @Autowired NotebookGroupService notebookGroupService;
  @Autowired ObjectMapper objectMapper;
  Note topNote;
  @MockitoBean EmbeddingService embeddingService;

  static NotebookSettings copyNotebookSettings(Notebook notebook) {
    var s = new NotebookSettings();
    var cur = notebook.getNotebookSettings();
    s.setSkipMemoryTrackingEntirely(cur.getSkipMemoryTrackingEntirely());
    return s;
  }

  static Integer catalogItemNotebookId(NotebookCatalogItem item) {
    return switch (item) {
      case NotebookCatalogNotebookItem n -> n.notebook.getId();
      case NotebookCatalogSubscribedNotebookItem s -> s.notebook.getId();
      case NotebookCatalogGroupItem g -> null;
    };
  }

  protected Notebook ownedNotebook() {
    return makeMe.aNotebook().creatorAndOwner(currentUser.getUser()).please();
  }

  protected Notebook ownedNotebook(String name) {
    return makeMe.aNotebook().name(name).creatorAndOwner(currentUser.getUser()).please();
  }

  protected Folder ownedFolder(Notebook notebook, String name) {
    return makeMe.aFolder().notebook(notebook).name(name).please();
  }

  @BeforeEach
  void setup() {
    when(embeddingService.streamEmbeddingsForNoteList(ArgumentMatchers.any()))
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
    topNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
  }
}

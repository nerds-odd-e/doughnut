package com.odde.doughnut.controllers;

import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.dto.NotebookCatalogGroupItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogNotebookItem;
import com.odde.doughnut.controllers.dto.NotebookCatalogSubscribedNotebookItem;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookSettings;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.entities.repositories.NotebookRepository;
import com.odde.doughnut.services.EmbeddingService;
import com.odde.doughnut.services.NoteService;
import com.odde.doughnut.services.NotebookGroupService;
import com.odde.doughnut.testability.builders.NoteBuilder;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

abstract class NotebookControllerTestBase extends ControllerTestBase {

  @Autowired
  com.odde.doughnut.entities.repositories.BazaarNotebookRepository bazaarNotebookRepository;

  @Autowired NotebookController controller;
  @Autowired NoteRepository noteRepository;
  @Autowired NotebookRepository notebookRepository;
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
    NoteBuilder noteBuilder = makeMe.aNote();
    topNote = noteBuilder.nbCreatorAndOwner(currentUser.getUser()).please();
  }
}

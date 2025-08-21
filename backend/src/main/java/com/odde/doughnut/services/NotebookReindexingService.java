package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NoteEmbeddingJdbcRepository;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotebookReindexingService {
  private final EmbeddingService embeddingService;
  private final NoteEmbeddingService noteEmbeddingService;
  private final NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository;
  private final ModelFactoryService modelFactoryService;

  public NotebookReindexingService(
      EmbeddingService embeddingService,
      NoteEmbeddingService noteEmbeddingService,
      NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository,
      ModelFactoryService modelFactoryService) {
    this.embeddingService = embeddingService;
    this.noteEmbeddingService = noteEmbeddingService;
    this.noteEmbeddingJdbcRepository = noteEmbeddingJdbcRepository;
    this.modelFactoryService = modelFactoryService;
  }

  public void reindexNotebook(Notebook notebook) {
    // Delete all existing embeddings for this notebook
    noteEmbeddingService.deleteNotebookEmbeddings(notebook.getId());
    // Delegate to incremental updater which will now re-embed all notes
    updateNotebookIndex(notebook);
  }

  /**
   * Update embeddings for notes whose content has changed since the last embedding generation, or
   * for notes that do not yet have any embeddings.
   */
  public void updateNotebookIndex(Notebook notebook) {
    List<Integer> candidateIds =
        noteEmbeddingJdbcRepository.selectNoteIdsNeedingIndexUpdateByNotebookId(notebook.getId());

    if (candidateIds.isEmpty()) return;

    List<Note> candidates =
        (List<Note>) modelFactoryService.noteRepository.findAllById(candidateIds);

    embeddingService
        .streamEmbeddingsForNoteList(candidates)
        .forEach(
            item ->
                item.embedding()
                    .ifPresent(
                        embedding -> noteEmbeddingService.storeEmbedding(item.note(), embedding)));
  }
}

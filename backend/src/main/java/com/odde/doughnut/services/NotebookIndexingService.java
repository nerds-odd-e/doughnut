package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.NoteEmbeddingJdbcRepository;
import com.odde.doughnut.entities.repositories.NoteRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotebookIndexingService {
  private final EmbeddingService embeddingService;
  private final NoteEmbeddingService noteEmbeddingService;
  private final NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository;
  private final NoteRepository noteRepository;

  public NotebookIndexingService(
      EmbeddingService embeddingService,
      NoteEmbeddingService noteEmbeddingService,
      NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository,
      NoteRepository noteRepository) {
    this.embeddingService = embeddingService;
    this.noteEmbeddingService = noteEmbeddingService;
    this.noteEmbeddingJdbcRepository = noteEmbeddingJdbcRepository;
    this.noteRepository = noteRepository;
  }

  /** Reset the index for a notebook by deleting all embeddings without regenerating them. */
  public void resetNotebookIndex(Notebook notebook) {
    noteEmbeddingService.deleteNotebookEmbeddings(notebook.getId());
  }

  /**
   * Update embeddings for notes whose content has changed since the last embedding generation, or
   * for notes that do not yet have any embeddings.
   */
  public void updateNotebookIndex(Notebook notebook) {
    List<Integer> candidateIds =
        noteEmbeddingJdbcRepository.selectNoteIdsNeedingIndexUpdateByNotebookId(notebook.getId());

    if (candidateIds.isEmpty()) return;

    List<Note> candidates = (List<Note>) noteRepository.findAllById(candidateIds);

    embeddingService
        .streamEmbeddingsForNoteList(candidates)
        .forEach(
            item ->
                item.embedding()
                    .ifPresent(
                        embedding -> noteEmbeddingService.storeEmbedding(item.note(), embedding)));
  }
}

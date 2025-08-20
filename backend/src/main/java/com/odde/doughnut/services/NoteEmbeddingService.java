package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteEmbedding;
import com.odde.doughnut.entities.repositories.NoteEmbeddingJdbcRepository;
import com.odde.doughnut.entities.repositories.NoteEmbeddingRepository;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class NoteEmbeddingService {
  private final NoteEmbeddingRepository noteEmbeddingRepository;
  private final NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository;
  private final ModelFactoryService modelFactoryService;

  public NoteEmbeddingService(
      NoteEmbeddingRepository noteEmbeddingRepository,
      NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository,
      ModelFactoryService modelFactoryService) {
    this.noteEmbeddingRepository = noteEmbeddingRepository;
    this.noteEmbeddingJdbcRepository = noteEmbeddingJdbcRepository;
    this.modelFactoryService = modelFactoryService;
  }

  public void storeEmbedding(Note note, List<Float> embedding) {
    // Store title embedding
    noteEmbeddingJdbcRepository.insert(
        note.getId(), NoteEmbedding.EmbeddingKind.TITLE.name(), embedding);

    // Store details embedding if note has details
    if (note.getDetails() != null && !note.getDetails().trim().isEmpty()) {
      noteEmbeddingJdbcRepository.insert(
          note.getId(), NoteEmbedding.EmbeddingKind.DETAILS.name(), embedding);
    }
  }

  public void deleteEmbedding(Integer noteId) {
    noteEmbeddingRepository.deleteByNoteId(noteId);
  }

  public void deleteNotebookEmbeddings(Integer notebookId) {
    // Get all notes in the notebook and delete their embeddings
    List<Note> notes =
        modelFactoryService
            .notebookRepository
            .findById(notebookId)
            .map(notebook -> notebook.getNotes())
            .orElse(List.of());

    for (Note note : notes) {
      deleteEmbedding(note.getId());
    }
  }

  public Optional<List<Float>> getEmbedding(Integer noteId, NoteEmbedding.EmbeddingKind kind) {
    // Prefer lightweight JDBC read for cross-env compatibility
    return noteEmbeddingJdbcRepository
        .select(noteId, kind.name())
        .map(
            bytes -> {
              NoteEmbedding ne = new NoteEmbedding();
              ne.setEmbedding(bytes);
              return ne.getEmbeddingAsFloats();
            });
  }
}

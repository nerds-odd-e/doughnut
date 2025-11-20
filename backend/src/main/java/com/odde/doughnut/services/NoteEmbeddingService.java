package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteEmbedding;
import com.odde.doughnut.entities.repositories.NoteEmbeddingJdbcRepository;
import com.odde.doughnut.entities.repositories.NoteEmbeddingRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class NoteEmbeddingService {
  private final NoteEmbeddingRepository noteEmbeddingRepository;
  private final NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository;

  public NoteEmbeddingService(
      NoteEmbeddingRepository noteEmbeddingRepository,
      NoteEmbeddingJdbcRepository noteEmbeddingJdbcRepository) {
    this.noteEmbeddingRepository = noteEmbeddingRepository;
    this.noteEmbeddingJdbcRepository = noteEmbeddingJdbcRepository;
  }

  public void storeEmbedding(Note note, List<Float> embedding) {
    noteEmbeddingJdbcRepository.insert(note.getId(), embedding);
  }

  public void deleteEmbedding(Integer noteId) {
    noteEmbeddingRepository.deleteByNoteId(noteId);
  }

  public void deleteNotebookEmbeddings(Integer notebookId) {
    noteEmbeddingRepository.deleteByNotebookId(notebookId);
  }

  public Optional<List<Float>> getEmbedding(Integer noteId) {
    return noteEmbeddingJdbcRepository
        .select(noteId)
        .map(
            bytes -> {
              NoteEmbedding ne = new NoteEmbedding();
              ne.setEmbedding(bytes);
              return ne.getEmbeddingAsFloats();
            });
  }
}

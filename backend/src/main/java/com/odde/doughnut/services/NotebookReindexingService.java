package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class NotebookReindexingService {
  private final EmbeddingService embeddingService;
  private final RedisVectorService redisVectorService;

  public NotebookReindexingService(
      EmbeddingService embeddingService, RedisVectorService redisVectorService) {
    this.embeddingService = embeddingService;
    this.redisVectorService = redisVectorService;
  }

  public void reindexNotebook(Notebook notebook) {
    // Delete all existing embeddings for this notebook
    redisVectorService.deleteNotebookEmbeddings(notebook.getId());

    // Generate and store new embeddings for all notes in the notebook
    List<Note> notes = notebook.getNotes();
    for (Note note : notes) {
      reindexNote(note);
    }
  }

  private void reindexNote(Note note) {
    Optional<List<Float>> embedding = embeddingService.generateEmbedding(note);
    if (embedding.isPresent()) {
      redisVectorService.storeEmbedding(note, embedding.get());
    }
  }
}

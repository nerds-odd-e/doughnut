package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class NotebookReindexingService {
  private final EmbeddingService embeddingService;
  private final NoteEmbeddingService noteEmbeddingService;

  public NotebookReindexingService(
      EmbeddingService embeddingService, NoteEmbeddingService noteEmbeddingService) {
    this.embeddingService = embeddingService;
    this.noteEmbeddingService = noteEmbeddingService;
  }

  public void reindexNotebook(Notebook notebook) {
    // Delete all existing embeddings for this notebook
    noteEmbeddingService.deleteNotebookEmbeddings(notebook.getId());

    // Generate and store new embeddings for all notes in the notebook
    List<Note> notes = notebook.getNotes();
    embeddingService
        .streamEmbeddingsForNoteList(notes)
        .forEach(
            item ->
                item.embedding()
                    .ifPresent(
                        embedding -> noteEmbeddingService.storeEmbedding(item.note(), embedding)));
  }

  private void reindexNote(Note note) {
    Optional<List<Float>> embedding = embeddingService.generateEmbedding(note);
    embedding.ifPresent(list -> noteEmbeddingService.storeEmbedding(note, list));
  }
}

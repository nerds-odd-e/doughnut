package com.odde.doughnut.services;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class NoteEmbeddingService {
  private final ModelFactoryService modelFactoryService;

  public NoteEmbeddingService(ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  public void storeEmbedding(Note note, List<Float> embedding) {
    modelFactoryService.storeNoteEmbedding(note, embedding);
  }

  public void deleteEmbedding(Integer noteId) {
    modelFactoryService.deleteNoteEmbeddingByNoteId(noteId);
  }

  public void deleteNotebookEmbeddings(Integer notebookId) {
    modelFactoryService.deleteNoteEmbeddingsByNotebookId(notebookId);
  }

  public Optional<List<Float>> getEmbedding(Integer noteId) {
    return modelFactoryService.getNoteEmbeddingAsFloats(noteId);
  }
}

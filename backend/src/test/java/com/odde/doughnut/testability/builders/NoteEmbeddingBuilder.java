package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteEmbedding;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;

public class NoteEmbeddingBuilder extends EntityBuilder<NoteEmbedding> {
  private final Note note;
  private List<Float> embedding = List.of(0.0f);

  public NoteEmbeddingBuilder(Note note, MakeMe makeMe) {
    super(makeMe, new NoteEmbedding());
    this.note = note;
  }

  // kind removed; single embedding per note

  public NoteEmbeddingBuilder embedding(List<Float> embedding) {
    this.embedding = embedding;
    return this;
  }

  @Override
  protected void beforeCreate(boolean needPersist) {
    // no-op; insert is handled in please() to avoid JPA entity persistence
  }

  @Override
  public NoteEmbedding please() {
    return please(true);
  }

  @Override
  public NoteEmbedding please(boolean persistNeeded) {
    // Insert via the service to respect env-specific column types
    makeMe.noteEmbeddingService.storeEmbedding(note, embedding);
    // Return transient entity (not actually persisted via JPA)
    return entity;
  }
}

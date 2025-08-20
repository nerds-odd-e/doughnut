package com.odde.doughnut.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "note_embeddings")
public class NoteEmbedding extends EntityIdentifiedByIdOnly {

  @ManyToOne
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @Getter
  @Setter
  private Note note;

  @Enumerated(EnumType.STRING)
  @Column(name = "kind")
  @Getter
  @Setter
  private EmbeddingKind kind;

  @Transient
  @Getter
  @Setter
  private byte[] embedding;

  @Column(name = "created_at")
  @Getter
  @Setter
  private Timestamp createdAt;

  @Column(name = "updated_at")
  @Getter
  @Setter
  private Timestamp updatedAt;

  public enum EmbeddingKind {
    TITLE,
    DETAILS
  }

  public void setEmbeddingFromFloats(List<Float> floats) {
    this.embedding = new byte[floats.size() * 4];
    for (int i = 0; i < floats.size(); i++) {
      int intBits = Float.floatToIntBits(floats.get(i));
      this.embedding[i * 4] = (byte) (intBits >> 24);
      this.embedding[i * 4 + 1] = (byte) (intBits >> 16);
      this.embedding[i * 4 + 2] = (byte) (intBits >> 8);
      this.embedding[i * 4 + 3] = (byte) intBits;
    }
  }

  public List<Float> getEmbeddingAsFloats() {
    List<Float> floats = new java.util.ArrayList<>();
    for (int i = 0; i < embedding.length; i += 4) {
      int intBits =
          ((embedding[i] & 0xFF) << 24)
              | ((embedding[i + 1] & 0xFF) << 16)
              | ((embedding[i + 2] & 0xFF) << 8)
              | (embedding[i + 3] & 0xFF);
      floats.add(Float.intBitsToFloat(intBits));
    }
    return floats;
  }

  @PrePersist
  public void onCreate() {
    createdAt = new Timestamp(System.currentTimeMillis());
    updatedAt = createdAt;
  }

  @PreUpdate
  public void onUpdate() {
    updatedAt = new Timestamp(System.currentTimeMillis());
  }
}

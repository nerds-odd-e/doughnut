package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NoteEmbedding;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface NoteEmbeddingRepository extends CrudRepository<NoteEmbedding, Integer> {

  void deleteByNoteId(Integer noteId);

  void deleteByNoteIdAndKind(Integer noteId, NoteEmbedding.EmbeddingKind kind);

  Optional<NoteEmbedding> findByNoteIdAndKind(Integer noteId, NoteEmbedding.EmbeddingKind kind);
}

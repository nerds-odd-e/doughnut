package com.odde.donut.entities.repositories;

import com.odde.donut.entities.NoteEmbedding;
import org.springframework.data.repository.CrudRepository;

public interface NoteEmbeddingRepository extends CrudRepository<NoteEmbedding, Integer> {

  void deleteByNoteId(Integer noteId);

  boolean existsByNoteId(Integer noteId);
}

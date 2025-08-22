package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NoteEmbedding;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NoteEmbeddingRepository extends CrudRepository<NoteEmbedding, Integer> {

  void deleteByNoteId(Integer noteId);

  boolean existsByNoteId(Integer noteId);

  @Modifying
  @Query("DELETE FROM NoteEmbedding ne WHERE ne.note.notebook.id = :notebookId")
  void deleteByNotebookId(@Param("notebookId") Integer notebookId);
}

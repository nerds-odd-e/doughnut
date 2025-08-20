package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NoteEmbedding;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NoteEmbeddingRepository extends CrudRepository<NoteEmbedding, Integer> {

  void deleteByNoteId(Integer noteId);

  void deleteByNoteIdAndKind(Integer noteId, NoteEmbedding.EmbeddingKind kind);

  Optional<NoteEmbedding> findByNoteIdAndKind(Integer noteId, NoteEmbedding.EmbeddingKind kind);

  @Modifying
  @Query("DELETE FROM NoteEmbedding ne WHERE ne.note.notebook.id = :notebookId")
  void deleteByNotebookId(@Param("notebookId") Integer notebookId);
}

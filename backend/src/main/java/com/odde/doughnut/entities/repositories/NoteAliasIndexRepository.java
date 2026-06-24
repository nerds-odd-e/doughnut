package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NoteAliasIndex;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteAliasIndexRepository extends JpaRepository<NoteAliasIndex, Integer> {

  @Modifying
  @Query("DELETE FROM NoteAliasIndex i WHERE i.note.id = :noteId")
  void deleteByNoteIdInBulk(@Param("noteId") Integer noteId);

  List<NoteAliasIndex> findByNote_IdOrderByIdAsc(Integer noteId);
}

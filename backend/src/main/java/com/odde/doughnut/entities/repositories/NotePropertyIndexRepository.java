package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NotePropertyIndex;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotePropertyIndexRepository extends JpaRepository<NotePropertyIndex, Integer> {

  @Modifying
  @Query("DELETE FROM NotePropertyIndex i WHERE i.note.id = :noteId")
  void deleteByNoteIdInBulk(@Param("noteId") Integer noteId);

  List<NotePropertyIndex> findByNote_IdOrderByIdAsc(Integer noteId);
}

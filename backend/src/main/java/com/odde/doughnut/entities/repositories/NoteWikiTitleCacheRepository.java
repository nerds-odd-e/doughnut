package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NoteWikiTitleCache;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteWikiTitleCacheRepository extends JpaRepository<NoteWikiTitleCache, Integer> {

  void deleteByNote_Id(Integer noteId);

  List<NoteWikiTitleCache> findByNote_IdOrderByIdAsc(Integer noteId);

  @Query(
      "SELECT c FROM NoteWikiTitleCache c JOIN c.note n WHERE c.targetNote.id = :targetNoteId AND"
          + " n.deletedAt IS NULL ORDER BY n.id ASC, c.id ASC")
  List<NoteWikiTitleCache> findRowsReferringToNonDeletedNotesForTarget(
      @Param("targetNoteId") Integer targetNoteId);
}

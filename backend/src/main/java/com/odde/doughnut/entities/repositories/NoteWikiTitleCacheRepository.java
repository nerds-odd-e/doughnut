package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteWikiTitleCache;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteWikiTitleCacheRepository extends JpaRepository<NoteWikiTitleCache, Integer> {

  void deleteByNote_Id(Integer noteId);

  @Modifying
  @Query("DELETE FROM NoteWikiTitleCache c WHERE c.note.id = :noteId")
  void deleteByNoteIdInBulk(@Param("noteId") Integer noteId);

  List<NoteWikiTitleCache> findByNote_IdOrderByIdAsc(Integer noteId);

  @Query(
      "SELECT c FROM NoteWikiTitleCache c JOIN c.note n WHERE c.targetNote.id = :targetNoteId AND"
          + " n.deletedAt IS NULL ORDER BY n.id ASC, c.id ASC")
  List<NoteWikiTitleCache> findRowsReferringToNonDeletedNotesForTarget(
      @Param("targetNoteId") Integer targetNoteId);

  /**
   * Distinct non-deleted referrer notes linking to {@code targetNoteId}, visible per focus-context
   * inbound rules (same notebook as focal, or viewer owns/subscribes to referrer notebook).
   * Excludes {@code excludeIds} before applying {@code limit}. Order: referrer id ascending.
   */
  @Query(
      value =
          "SELECT r.* FROM note r INNER JOIN ("
              + "SELECT c.note_id FROM note_wiki_title_cache c "
              + "INNER JOIN note r2 ON r2.id = c.note_id AND r2.deleted_at IS NULL "
              + "WHERE c.target_note_id = :targetNoteId GROUP BY c.note_id) dedup "
              + "ON dedup.note_id = r.id "
              + "WHERE r.deleted_at IS NULL AND r.id NOT IN (:excludeIds) AND ("
              + "(:focalNotebookId IS NOT NULL AND r.notebook_id = :focalNotebookId) OR "
              + "(:viewerId IS NOT NULL AND ("
              + "EXISTS (SELECT 1 FROM notebook nb INNER JOIN ownership o ON o.id = nb.ownership_id "
              + "WHERE nb.id = r.notebook_id AND o.user_id = :viewerId) OR "
              + "EXISTS (SELECT 1 FROM subscription s WHERE s.notebook_id = r.notebook_id "
              + "AND s.user_id = :viewerId)))) "
              + "ORDER BY r.id ASC LIMIT :limit",
      nativeQuery = true)
  List<Note> findInboundReferrersForTargetByIdAscLimited(
      @Param("targetNoteId") Integer targetNoteId,
      @Param("focalNotebookId") Integer focalNotebookId,
      @Param("viewerId") Integer viewerId,
      @Param("excludeIds") List<Integer> excludeIds,
      @Param("limit") int limit);

  @Query(
      value =
          "SELECT r.* FROM note r INNER JOIN ("
              + "SELECT c.note_id FROM note_wiki_title_cache c "
              + "INNER JOIN note r2 ON r2.id = c.note_id AND r2.deleted_at IS NULL "
              + "WHERE c.target_note_id = :targetNoteId GROUP BY c.note_id) dedup "
              + "ON dedup.note_id = r.id "
              + "WHERE r.deleted_at IS NULL AND r.id NOT IN (:excludeIds) AND ("
              + "(:focalNotebookId IS NOT NULL AND r.notebook_id = :focalNotebookId) OR "
              + "(:viewerId IS NOT NULL AND ("
              + "EXISTS (SELECT 1 FROM notebook nb INNER JOIN ownership o ON o.id = nb.ownership_id "
              + "WHERE nb.id = r.notebook_id AND o.user_id = :viewerId) OR "
              + "EXISTS (SELECT 1 FROM subscription s WHERE s.notebook_id = r.notebook_id "
              + "AND s.user_id = :viewerId)))) "
              + "ORDER BY CRC32(CONCAT(CAST(r.id AS CHAR), CAST(:seed AS CHAR))) ASC LIMIT :limit",
      nativeQuery = true)
  List<Note> findInboundReferrersForTargetBySeedLimited(
      @Param("targetNoteId") Integer targetNoteId,
      @Param("focalNotebookId") Integer focalNotebookId,
      @Param("viewerId") Integer viewerId,
      @Param("excludeIds") List<Integer> excludeIds,
      @Param("seed") String seed,
      @Param("limit") int limit);
}

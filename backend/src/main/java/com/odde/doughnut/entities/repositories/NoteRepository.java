package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends CrudRepository<Note, Integer> {

  @Query(value = selectFromNote + " where title = :noteTitle limit 1", nativeQuery = true)
  Note findFirstByTitle(@Param("noteTitle") String noteTitle);

  String selectFromNote = "SELECT note.*  from note";

  @Query(value = inAllMyNotebooksAndSubscriptions + searchForLinkTarget, nativeQuery = true)
  List<Note> searchForUserInAllMyNotebooksAndSubscriptions(
      Integer userId, @Param("pattern") String pattern);

  @Query(value = inAllMyNotebooksSubscriptionsAndCircles + searchForLinkTarget, nativeQuery = true)
  List<Note> searchForUserInAllMyNotebooksSubscriptionsAndCircle(
      Integer userId, @Param("pattern") String pattern);

  @Query(
      value = selectFromNote + " WHERE note.notebook_id = :notebookId " + searchForLinkTarget,
      nativeQuery = true)
  List<Note> searchInNotebook(Integer notebookId, @Param("pattern") String pattern);

  @Query(
      value =
          selectFromNote
              + " WHERE note.notebook_id = :notebookId "
              + " AND note.wikidata_id = :wikidataId AND note.wikidata_id IS NOT NULL AND note.deleted_at IS NULL ",
      nativeQuery = true)
  List<Note> noteWithWikidataIdWithinNotebook(
      @Param("notebookId") Integer notebookId, @Param("wikidataId") String wikidataId);

  String joinNotebooksBegin =
      selectFromNote + "  JOIN (" + "          SELECT notebook.id FROM notebook ";

  String joinNotebooksEnd =
      "          UNION "
          + "          SELECT notebook_id FROM subscription "
          + "             WHERE subscription.user_id = :userId "
          + "       ) nb ON nb.id = note.notebook_id "
          + "  WHERE 1=1 ";

  String inAllMyNotebooksAndSubscriptions =
      joinNotebooksBegin
          + "             JOIN ownership ON ownership.user_id = :userId "
          + "             WHERE notebook.ownership_id = ownership.id "
          + joinNotebooksEnd;

  String inAllMyNotebooksSubscriptionsAndCircles =
      joinNotebooksBegin
          + "             LEFT JOIN circle_user ON circle_user.user_id = :userId "
          + "             LEFT JOIN circle ON circle.id = circle_user.circle_id "
          + "             JOIN ownership ON circle.id = ownership.circle_id OR ownership.user_id = :userId "
          + "             WHERE notebook.ownership_id = ownership.id "
          + joinNotebooksEnd;

  String searchForLinkTarget = " AND REGEXP_LIKE(title, :pattern) AND note.deleted_at IS NULL ";

  @Modifying
  @Query(
      value =
          " UPDATE note JOIN notes_closure ON notes_closure.note_id = note.id AND notes_closure.ancestor_id = :#{#note.id} SET deleted_at = :currentUTCTimestamp WHERE deleted_at IS NULL",
      nativeQuery = true)
  void softDeleteDescendants(
      @Param("note") Note note, @Param("currentUTCTimestamp") Timestamp currentUTCTimestamp);

  @Modifying
  @Query(
      value =
          " UPDATE note JOIN notes_closure ON notes_closure.note_id = note.id AND notes_closure.ancestor_id = :#{#note.id} SET deleted_at = NULL WHERE deleted_at = :currentUTCTimestamp",
      nativeQuery = true)
  void undoDeleteDescendants(
      @Param("note") Note note, @Param("currentUTCTimestamp") Timestamp currentUTCTimestamp);

  @Query(
      value =
          """
      SELECT
          note.*
      FROM
          notes_closure
      JOIN
          note ON note.id = notes_closure.note_id
      WHERE
          notes_closure.ancestor_id = ? AND
          note.deleted_at IS NULL
      ORDER BY
          notes_closure.depth,
          note.sibling_order
      """,
      nativeQuery = true)
  List<Note> getDescendantsInBreathFirstOrder(Integer ancestorId);
}

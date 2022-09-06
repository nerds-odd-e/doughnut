package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends CrudRepository<Note, Integer> {

  @Query(
      value = selectFromNoteJoinTextContent + " where text_content.title = :noteTitle limit 1",
      nativeQuery = true)
  Note findFirstByTitle(@Param("noteTitle") String noteTitle);

  String selectFromNoteJoinTextContent =
      "SELECT note.*  from note JOIN text_content"
          + "   ON note.text_content_id = text_content.id ";

  @Query(value = inAllMyNotebooksAndSubscriptions + searchForLinkTarget, nativeQuery = true)
  List<Note> searchForUserInAllMyNotebooksAndSubscriptions(
      @Param("user") User user, @Param("pattern") String pattern);

  @Query(value = inAllMyNotebooksSubscriptionsAndCircles + searchForLinkTarget, nativeQuery = true)
  List<Note> searchForUserInAllMyNotebooksSubscriptionsAndCircle(
      @Param("user") User user, @Param("pattern") String pattern);

  @Query(
      value =
          selectFromNoteJoinTextContent
              + " WHERE note.notebook_id = :notebook "
              + searchForLinkTarget,
      nativeQuery = true)
  List<Note> searchInNotebook(
      @Param("notebook") Notebook notebook, @Param("pattern") String pattern);

  @Query(
    value =
      selectFromNoteJoinTextContent
        + " WHERE note.notebook_id = :notebook "
        + searchForExistingWikidataId,
    nativeQuery = true)
  List<Note> searchInNotebookByWikidataId(
    @Param("notebook") Notebook notebook, @Param("wikidataId") String wikidataId);

  String joinNotebooksBegin =
      selectFromNoteJoinTextContent + "  JOIN (" + "          SELECT notebook.id FROM notebook ";

  String joinNotebooksEnd =
      "          UNION "
          + "          SELECT notebook_id FROM subscription "
          + "             WHERE subscription.user_id = :user "
          + "       ) nb ON nb.id = note.notebook_id "
          + "  WHERE 1=1 ";

  String inAllMyNotebooksAndSubscriptions =
      joinNotebooksBegin
          + "             JOIN ownership ON ownership.user_id = :user "
          + "             WHERE notebook.ownership_id = ownership.id "
          + joinNotebooksEnd;

  String inAllMyNotebooksSubscriptionsAndCircles =
      joinNotebooksBegin
          + "             LEFT JOIN circle_user ON circle_user.user_id = :user "
          + "             LEFT JOIN circle ON circle.id = circle_user.circle_id "
          + "             JOIN ownership ON circle.id = ownership.circle_id OR ownership.user_id = :user "
          + "             WHERE notebook.ownership_id = ownership.id "
          + joinNotebooksEnd;

  String searchForLinkTarget =
      " AND REGEXP_LIKE(text_content.title, :pattern) AND note.deleted_at IS NULL ";

  String searchForExistingWikidataId =
    " AND note.wikidata_id = :wikidataId AND note.deleted_at IS NULL ";

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

  @Query(value = "SELECT note.* FROM note where id in (:ids)", nativeQuery = true)
  Stream<Note> findAllByIds(List<Integer> ids);
}

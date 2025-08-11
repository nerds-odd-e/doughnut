package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends CrudRepository<Note, Integer> {
  String selectFromNote = "SELECT n FROM Note n";
  String searchForTitleLike = " WHERE n.topicConstructor LIKE :pattern AND n.deletedAt IS NULL ";
  String searchForTitleExact = " WHERE LOWER(n.topicConstructor) = LOWER(:key) AND n.deletedAt IS NULL ";

  @Query(value = selectFromNote + " where n.topicConstructor = :key")
  Note findFirstByTitle(@Param("key") String key);

  @Query(
      value =
          selectFromNote
              + " WHERE n.notebook.id = ("
              + "SELECT nhn.notebook_id FROM NotebookHeadNote nhn WHERE nhn.head_note_id = (SELECT rn.id FROM Note rn WHERE rn.topicConstructor = :title)"
              + ") AND n.topicConstructor = :key")
  Note findFirstInNotebookByTitle(@Param("title") String notebookTitle, @Param("key") String key);

  @Query(
      value = selectFromNote + searchForTitleLike + "  AND n.notebook.ownership.user.id = :userId")
  List<Note> searchForUserInAllMyNotebooks(Integer userId, String pattern, Pageable pageable);

  @Query(
      value = selectFromNote + searchForTitleExact + "  AND n.notebook.ownership.user.id = :userId")
  List<Note> searchExactForUserInAllMyNotebooks(Integer userId, String key);

  @Query(
      value =
          selectFromNote
              + " JOIN n.notebook.subscriptions s ON s.user.id = :userId "
              + searchForTitleExact)
  List<Note> searchExactForUserInAllMySubscriptions(Integer userId, @Param("key") String key);

  @Query(
      value =
          selectFromNote
              + "              JOIN n.notebook.ownership.circle.members m"
              + "                ON m.id = :userId "
              + searchForTitleExact)
  List<Note> searchExactForUserInAllMyCircle(Integer userId, @Param("key") String key);

  @Query(value = selectFromNote + searchForTitleExact + " AND n.notebook.id = :notebookId")
  List<Note> searchExactInNotebook(Integer notebookId, @Param("key") String key);

  @Query(
      value =
          selectFromNote
              + " JOIN n.notebook.subscriptions s ON s.user.id = :userId "
              + searchForTitleLike)
  List<Note> searchForUserInAllMySubscriptions(
      Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query(
      value =
          selectFromNote
              + "              JOIN n.notebook.ownership.circle.members m"
              + "                ON m.id = :userId "
              + searchForTitleLike)
  List<Note> searchForUserInAllMyCircle(
      Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query(value = selectFromNote + searchForTitleLike + " AND n.notebook.id = :notebookId")
  List<Note> searchInNotebook(
      Integer notebookId, @Param("pattern") String pattern, Pageable pageable);

  @Query(
      value =
          selectFromNote
              + " WHERE n.notebook.id = :notebookId "
              + " AND n.wikidataId = :wikidataId AND n.wikidataId IS NOT NULL AND n.deletedAt IS NULL ")
  List<Note> noteWithWikidataIdWithinNotebook(
      @Param("notebookId") Integer notebookId, @Param("wikidataId") String wikidataId);

  @Query(
      value =
          selectFromNote
              + " WHERE n.notebook.ownership.user.id = :userId"
              + " AND n.deletedAt IS NULL"
              + " ORDER BY n.updatedAt DESC"
              + " LIMIT 100")
  List<Note> findRecentNotesByUser(@Param("userId") Integer userId);
}

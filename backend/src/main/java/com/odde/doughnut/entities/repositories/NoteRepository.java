package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends CrudRepository<Note, Integer> {

  String selectFromNote = "SELECT n FROM Note n";
  String searchForTopicLike = " WHERE n.topicConstructor LIKE :pattern AND n.deletedAt IS NULL ";

  @Query(value = selectFromNote + " WHERE n.id IN (:ids)")
  Stream<Note> findAllByIds(List<Integer> ids);

  @Query(value = selectFromNote + " where n.topicConstructor = :key")
  Note findFirstByTopicConstructor(@Param("key") String key);

  @Query(
      value =
          selectFromNote
              + " WHERE n.notebook.id = ("
              + "SELECT nhn.notebook_id FROM NotebookHeadNote nhn WHERE nhn.head_note_id = (SELECT rn.id FROM Note rn WHERE rn.topicConstructor = :title)"
              + ") AND n.topicConstructor = :key")
  Note findFirstInNotebookByTopicConstructor(
      @Param("title") String notebookTitle, @Param("key") String key);

  @Query(
      value = selectFromNote + searchForTopicLike + "  AND n.notebook.ownership.user.id = :userId ")
  Stream<Note> searchForUserInAllMyNotebooks(Integer userId, String pattern);

  @Query(
      value =
          selectFromNote
              + " JOIN n.notebook.subscriptions s ON s.user.id = :userId "
              + searchForTopicLike)
  Stream<Note> searchForUserInAllMySubscriptions(Integer userId, @Param("pattern") String pattern);

  @Query(
      value =
          selectFromNote
              + "              JOIN n.notebook.ownership.circle.members m"
              + "                ON m.id = :userId "
              + searchForTopicLike)
  Stream<Note> searchForUserInAllMyCircle(Integer userId, @Param("pattern") String pattern);

  @Query(value = selectFromNote + searchForTopicLike + " AND n.notebook.id = :notebookId ")
  Stream<Note> searchInNotebook(Integer notebookId, @Param("pattern") String pattern);

  @Query(
      value =
          selectFromNote
              + " WHERE n.notebook.id = :notebookId "
              + " AND n.wikidataId = :wikidataId AND n.wikidataId IS NOT NULL AND n.deletedAt IS NULL ")
  List<Note> noteWithWikidataIdWithinNotebook(
      @Param("notebookId") Integer notebookId, @Param("wikidataId") String wikidataId);
}

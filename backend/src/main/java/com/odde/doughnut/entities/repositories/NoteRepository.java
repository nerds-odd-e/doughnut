package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends CrudRepository<Note, Integer> {

  @Query(value = "SELECT note.* FROM note where id in (:ids)", nativeQuery = true)
  Stream<Note> findAllByIds(List<Integer> ids);

  @Query(value = selectFromNote + " where topic_constructor = :key limit 1", nativeQuery = true)
  Note findFirstByTopicConstructor(@Param("key") String key);

  String selectFromNote = "SELECT note.*  from note";

  @Query(value = inAllMyNotebooks + searchForTopicLike, nativeQuery = true)
  Stream<Note> searchForUserInAllMyNotebooks(Integer userId, String pattern);

  @Query(value = inAllMySubscriptions + searchForTopicLike, nativeQuery = true)
  Stream<Note> searchForUserInAllMySubscriptions(Integer userId, @Param("pattern") String pattern);

  @Query(value = inAllMyCircles + searchForTopicLike, nativeQuery = true)
  Stream<Note> searchForUserInAllMyCircle(Integer userId, @Param("pattern") String pattern);

  @Query(
      value = selectFromNote + searchForTopicLike + " AND note.notebook_id = :notebookId ",
      nativeQuery = true)
  Stream<Note> searchInNotebook(Integer notebookId, @Param("pattern") String pattern);

  @Query(
      value =
          selectFromNote
              + " WHERE note.notebook_id = :notebookId "
              + " AND note.wikidata_id = :wikidataId AND note.wikidata_id IS NOT NULL AND note.deleted_at IS NULL ",
      nativeQuery = true)
  List<Note> noteWithWikidataIdWithinNotebook(
      @Param("notebookId") Integer notebookId, @Param("wikidataId") String wikidataId);

  String joinNotebooksBegin = selectFromNote + "  JOIN notebook ON notebook.id = note.notebook_id ";

  String inAllMySubscriptions =
      selectFromNote
          + "  JOIN subscription "
          + "             ON subscription.user_id = :userId AND subscription.notebook_id = note.notebook_id ";

  String inAllMyNotebooks =
      joinNotebooksBegin
          + "             JOIN ownership ON ownership.user_id = :userId "
          + "             AND notebook.ownership_id = ownership.id ";

  String inAllMyCircles =
      joinNotebooksBegin
          + "             JOIN circle_user ON circle_user.user_id = :userId "
          + "             JOIN circle ON circle.id = circle_user.circle_id "
          + "             JOIN ownership ON circle.id = ownership.circle_id "
          + "             AND notebook.ownership_id = ownership.id ";

  String searchForTopicLike = " WHERE topic_constructor LIKE :pattern AND note.deleted_at IS NULL ";
}

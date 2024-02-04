package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NoteRepository extends CrudRepository<Note, Integer> {

  String selectFromNote1 = "SELECT n FROM Note n";

  @Query(value = selectFromNote1 + " WHERE n.id IN (:ids)")
  Stream<Note> findAllByIds(List<Integer> ids);

  String selectFromNote = "SELECT note.*  from note";

  @Query(value = selectFromNote1 + " where n.topicConstructor = :key")
  Note findFirstByTopicConstructor(@Param("key") String key);

  @Query(value = inAllMyNotebooks + searchForTopicLike1)
  Stream<Note> searchForUserInAllMyNotebooks(Integer userId, String pattern);

  @Query(value = inAllMySubscriptions + searchForTopicLike1)
  Stream<Note> searchForUserInAllMySubscriptions(Integer userId, @Param("pattern") String pattern);

  @Query(value = inAllMyCircles + searchForTopicLike1)
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

  String joinNotebooksBegin1 = selectFromNote1 + "  JOIN n.notebook nb ";

  String inAllMySubscriptions =
      joinNotebooksBegin1 + " JOIN nb.subscriptions s ON s.user.id = :userId ";

  String inAllMyNotebooks = joinNotebooksBegin1 + "             ON nb.ownership.user.id = :userId ";

  String inAllMyCircles =
      joinNotebooksBegin1
          + "            JOIN nb.ownership o "
          + "              JOIN o.circle c "
          + "              JOIN c.members m"
          + "                ON m.id = :userId ";

  String searchForTopicLike = " WHERE topic_constructor LIKE :pattern AND note.deleted_at IS NULL ";
  String searchForTopicLike1 = " WHERE n.topicConstructor LIKE :pattern AND n.deletedAt IS NULL ";
}

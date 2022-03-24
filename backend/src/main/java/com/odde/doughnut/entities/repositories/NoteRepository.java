package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Stream;

public interface NoteRepository extends CrudRepository<Note, Integer> {
    @Query(value = "SELECT note.*,rs.level as level from note " + byOwnershipWhereThereIsNoReviewPoint, nativeQuery = true)
    List<Note> findByOwnershipWhereThereIsNoReviewPoint(@Param("user") User user);

    @Query(value = "SELECT count(1) as count from note " + byOwnershipWhereThereIsNoReviewPoint, nativeQuery = true)
    int countByOwnershipWhereThereIsNoReviewPoint(@Param("user") User user);

    @Query(value = selectFromNoteJoinTextContent + " where text_content.title = :noteTitle limit 1", nativeQuery = true)
    Note findFirstByTitle(@Param("noteTitle") String noteTitle);

    @Query(value = "SELECT note.* from note " + byAncestorWhereThereIsNoReviewPoint, nativeQuery = true)
    List<Note> findByAncestorWhereThereIsNoReviewPoint(@Param("user") User user, @Param("ancestor") Note ancestor);

    @Query(value = "SELECT count(1) as count from note " + byAncestorWhereThereIsNoReviewPoint, nativeQuery = true)
    int countByAncestorWhereThereIsNoReviewPoint(@Param("user") User user, @Param("ancestor") Note ancestor);

    @Query(value = "SELECT count(1) as count from note " + joinClosure + " WHERE note.id in :noteIds", nativeQuery = true)
    int countByAncestorAndInTheList(@Param("ancestor") Note ancestor, @Param("noteIds") List<Integer> noteIds);

    String selectFromNoteJoinTextContent = "SELECT note.*  from note JOIN text_content"
            + "   ON note.text_content_id = text_content.id ";

    String whereThereIsNoReviewPointAndOrderByTime = " LEFT JOIN review_point rp"
            + "   ON note.id = rp.note_id "
            + "     AND rp.user_id = :#{#user.id} "
            + " LEFT JOIN review_setting rs "
            + "   ON note.master_review_setting_id = rs.id "
            + " WHERE note.skip_review IS FALSE "
            + "   AND rp.id IS NULL "
            + "   AND note.deleted_at IS NULL "
            + "ORDER BY level, note.created_at ";

    String byOwnershipWhereThereIsNoReviewPoint = "JOIN notebook ON notebook.id = note.notebook_id "
            + " AND notebook.ownership_id = :#{#user.ownership.id} "
            + whereThereIsNoReviewPointAndOrderByTime;

    String joinClosure = " JOIN notes_closure ON notes_closure.note_id = note.id "
            + "   AND notes_closure.ancestor_id = :ancestor ";

    String byAncestorWhereThereIsNoReviewPoint = joinClosure
            + whereThereIsNoReviewPointAndOrderByTime;

    @Query(value = inAllMyNotebooksAndSubscriptions + searchForLinkTarget, nativeQuery = true)
    List<Note> searchForUserInAllMyNotebooksAndSubscriptions(@Param("user") User user, @Param("pattern") String pattern);

    @Query(value = inAllMyNotebooksSubscriptionsAndCircles + searchForLinkTarget, nativeQuery = true)
    List<Note> searchForUserInAllMyNotebooksSubscriptionsAndCircle(@Param("user") User user, @Param("pattern") String pattern);

    @Query(value = selectFromNoteJoinTextContent + " WHERE note.notebook_id = :notebook " + searchForLinkTarget, nativeQuery = true)
    List<Note> searchInNotebook(@Param("notebook") Notebook notebook, @Param("pattern") String pattern);

    String joinNotebooksBegin = selectFromNoteJoinTextContent
            + "  JOIN ("
            + "          SELECT notebook.id FROM notebook ";

    String joinNotebooksEnd =  "          UNION "
            + "          SELECT notebook_id FROM subscription "
            + "             WHERE subscription.user_id = :user "
            + "       ) nb ON nb.id = note.notebook_id "
            + "  WHERE 1=1 ";



    String inAllMyNotebooksAndSubscriptions = joinNotebooksBegin
            + "             JOIN ownership ON ownership.user_id = :user "
            + "             WHERE notebook.ownership_id = ownership.id "
            + joinNotebooksEnd;

    String inAllMyNotebooksSubscriptionsAndCircles = joinNotebooksBegin
            + "             LEFT JOIN circle_user ON circle_user.user_id = :user "
            + "             LEFT JOIN circle ON circle.id = circle_user.circle_id "
            + "             JOIN ownership ON circle.id = ownership.circle_id OR ownership.user_id = :user "
            + "             WHERE notebook.ownership_id = ownership.id "
            + joinNotebooksEnd;

    String searchForLinkTarget = " AND REGEXP_LIKE(text_content.title, :pattern) ";

    @Modifying
    @Query(value = " UPDATE note JOIN notes_closure ON notes_closure.note_id = note.id AND notes_closure.ancestor_id = :#{#note.id} SET deleted_at = :currentUTCTimestamp WHERE deleted_at IS NULL", nativeQuery = true)
    void softDeleteDescendants(@Param("note") Note note, @Param("currentUTCTimestamp") Timestamp currentUTCTimestamp);

    @Modifying
    @Query(value = " UPDATE note JOIN notes_closure ON notes_closure.note_id = note.id AND notes_closure.ancestor_id = :#{#note.id} SET deleted_at = NULL WHERE deleted_at = :currentUTCTimestamp", nativeQuery = true)
    void undoDeleteDescendants(@Param("note") Note note, @Param("currentUTCTimestamp") Timestamp currentUTCTimestamp);

    @Query(value = "SELECT note.* FROM note where id in (:ids)", nativeQuery = true)
    Stream<Note> findAllByIds(String[] ids);
}

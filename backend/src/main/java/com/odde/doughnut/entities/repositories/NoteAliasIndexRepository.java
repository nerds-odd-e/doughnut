package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteAliasIndex;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteAliasIndexRepository extends JpaRepository<NoteAliasIndex, Integer> {

  String selectNoteFromAliasIndex = "SELECT DISTINCT n FROM NoteAliasIndex i JOIN i.note n";
  String aliasExactWhere =
      " WHERE i.aliasLookupKey = :lookupKey AND n.deletedAt IS NULL ";
  String aliasPartialWhere =
      " WHERE i.aliasLookupKey LIKE :pattern AND n.deletedAt IS NULL ";

  @Modifying
  @Query("DELETE FROM NoteAliasIndex i WHERE i.note.id = :noteId")
  void deleteByNoteIdInBulk(@Param("noteId") Integer noteId);

  List<NoteAliasIndex> findByNote_IdOrderByIdAsc(Integer noteId);

  @Query(
      value =
          "SELECT i FROM NoteAliasIndex i "
              + " JOIN FETCH i.note n "
              + " JOIN FETCH n.notebook nb "
              + " WHERE i.aliasLookupKey = :aliasLookupKey "
              + " AND n.deletedAt IS NULL "
              + " AND nb.deletedAt IS NULL "
              + " AND LOWER(nb.name) = LOWER(:notebookName) "
              + " ORDER BY n.id ASC")
  List<NoteAliasIndex> findByNotebookNameAndAliasLookupKeyOrderByNoteIdAsc(
      @Param("notebookName") String notebookName, @Param("aliasLookupKey") String aliasLookupKey);

  @Query(
      value =
          selectNoteFromAliasIndex
              + aliasExactWhere
              + " AND n.notebook.ownership.user.id = :userId")
  List<Note> searchExactForUserInAllMyNotebooks(
      @Param("userId") Integer userId, @Param("lookupKey") String lookupKey);

  @Query(
      value =
          selectNoteFromAliasIndex
              + " JOIN n.notebook.subscriptions s ON s.user.id = :userId "
              + aliasExactWhere)
  List<Note> searchExactForUserInAllMySubscriptions(
      @Param("userId") Integer userId, @Param("lookupKey") String lookupKey);

  @Query(
      value =
          selectNoteFromAliasIndex
              + " JOIN n.notebook.ownership.circle.members m ON m.id = :userId "
              + aliasExactWhere)
  List<Note> searchExactForUserInAllMyCircle(
      @Param("userId") Integer userId, @Param("lookupKey") String lookupKey);

  @Query(value = selectNoteFromAliasIndex + aliasExactWhere + " AND n.notebook.id = :notebookId")
  List<Note> searchExactInNotebook(
      @Param("notebookId") Integer notebookId, @Param("lookupKey") String lookupKey);

  @Query(
      value =
          selectNoteFromAliasIndex
              + aliasPartialWhere
              + " AND n.notebook.ownership.user.id = :userId")
  List<Note> searchForUserInAllMyNotebooks(
      @Param("userId") Integer userId,
      @Param("pattern") String pattern,
      Pageable pageable);

  @Query(
      value =
          selectNoteFromAliasIndex
              + " JOIN n.notebook.subscriptions s ON s.user.id = :userId "
              + aliasPartialWhere)
  List<Note> searchForUserInAllMySubscriptions(
      @Param("userId") Integer userId,
      @Param("pattern") String pattern,
      Pageable pageable);

  @Query(
      value =
          selectNoteFromAliasIndex
              + " JOIN n.notebook.ownership.circle.members m ON m.id = :userId "
              + aliasPartialWhere)
  List<Note> searchForUserInAllMyCircle(
      @Param("userId") Integer userId,
      @Param("pattern") String pattern,
      Pageable pageable);

  @Query(value = selectNoteFromAliasIndex + aliasPartialWhere + " AND n.notebook.id = :notebookId")
  List<Note> searchInNotebook(
      @Param("notebookId") Integer notebookId,
      @Param("pattern") String pattern,
      Pageable pageable);
}

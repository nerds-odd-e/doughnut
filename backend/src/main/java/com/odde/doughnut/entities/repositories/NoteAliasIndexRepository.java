package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NoteAliasIndex;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteAliasIndexRepository extends JpaRepository<NoteAliasIndex, Integer> {
  String SELECT_ALIAS_WITH_NOTEBOOK =
      "SELECT i FROM NoteAliasIndex i " + " JOIN FETCH i.note n " + " JOIN FETCH n.notebook nb ";
  String ACTIVE_NOTE_AND_NOTEBOOK = " AND n.deletedAt IS NULL " + " AND nb.deletedAt IS NULL ";

  @Modifying
  @Query("DELETE FROM NoteAliasIndex i WHERE i.note.id = :noteId")
  void deleteByNoteIdInBulk(@Param("noteId") Integer noteId);

  List<NoteAliasIndex> findByNote_IdOrderByIdAsc(Integer noteId);

  @Query(
      value =
          SELECT_ALIAS_WITH_NOTEBOOK
              + " WHERE i.aliasLookupKey = :aliasLookupKey "
              + ACTIVE_NOTE_AND_NOTEBOOK
              + " AND LOWER(nb.name) = LOWER(:notebookName) "
              + " ORDER BY n.id ASC")
  List<NoteAliasIndex> findByNotebookNameAndAliasLookupKeyOrderByNoteIdAsc(
      @Param("notebookName") String notebookName, @Param("aliasLookupKey") String aliasLookupKey);

  @Query(
      value =
          SELECT_ALIAS_WITH_NOTEBOOK
              + " WHERE i.aliasLookupKey = :aliasLookupKey "
              + ACTIVE_NOTE_AND_NOTEBOOK
              + " AND nb.ownership.user.id = :userId "
              + " ORDER BY n.id ASC")
  List<NoteAliasIndex> searchExactForUserInAllMyNotebooks(
      @Param("userId") Integer userId, @Param("aliasLookupKey") String aliasLookupKey);

  @Query(
      value =
          SELECT_ALIAS_WITH_NOTEBOOK
              + " JOIN nb.subscriptions s ON s.user.id = :userId "
              + " WHERE i.aliasLookupKey = :aliasLookupKey "
              + ACTIVE_NOTE_AND_NOTEBOOK
              + " ORDER BY n.id ASC")
  List<NoteAliasIndex> searchExactForUserInAllMySubscriptions(
      @Param("userId") Integer userId, @Param("aliasLookupKey") String aliasLookupKey);

  @Query(
      value =
          SELECT_ALIAS_WITH_NOTEBOOK
              + " JOIN nb.ownership.circle.members m ON m.id = :userId "
              + " WHERE i.aliasLookupKey = :aliasLookupKey "
              + ACTIVE_NOTE_AND_NOTEBOOK
              + " ORDER BY n.id ASC")
  List<NoteAliasIndex> searchExactForUserInAllMyCircle(
      @Param("userId") Integer userId, @Param("aliasLookupKey") String aliasLookupKey);

  @Query(
      value =
          SELECT_ALIAS_WITH_NOTEBOOK
              + " WHERE i.aliasLookupKey = :aliasLookupKey "
              + ACTIVE_NOTE_AND_NOTEBOOK
              + " AND nb.id = :notebookId "
              + " ORDER BY n.id ASC")
  List<NoteAliasIndex> searchExactInNotebook(
      @Param("notebookId") Integer notebookId, @Param("aliasLookupKey") String aliasLookupKey);

  @Query(
      value =
          SELECT_ALIAS_WITH_NOTEBOOK
              + " WHERE i.aliasLookupKey LIKE :pattern "
              + ACTIVE_NOTE_AND_NOTEBOOK
              + " AND nb.ownership.user.id = :userId "
              + " ORDER BY n.id ASC")
  List<NoteAliasIndex> searchForUserInAllMyNotebooks(
      @Param("userId") Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query(
      value =
          SELECT_ALIAS_WITH_NOTEBOOK
              + " JOIN nb.subscriptions s ON s.user.id = :userId "
              + " WHERE i.aliasLookupKey LIKE :pattern "
              + ACTIVE_NOTE_AND_NOTEBOOK
              + " ORDER BY n.id ASC")
  List<NoteAliasIndex> searchForUserInAllMySubscriptions(
      @Param("userId") Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query(
      value =
          SELECT_ALIAS_WITH_NOTEBOOK
              + " JOIN nb.ownership.circle.members m ON m.id = :userId "
              + " WHERE i.aliasLookupKey LIKE :pattern "
              + ACTIVE_NOTE_AND_NOTEBOOK
              + " ORDER BY n.id ASC")
  List<NoteAliasIndex> searchForUserInAllMyCircle(
      @Param("userId") Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query(
      value =
          SELECT_ALIAS_WITH_NOTEBOOK
              + " WHERE i.aliasLookupKey LIKE :pattern "
              + ACTIVE_NOTE_AND_NOTEBOOK
              + " AND nb.id = :notebookId "
              + " ORDER BY n.id ASC")
  List<NoteAliasIndex> searchInNotebook(
      @Param("notebookId") Integer notebookId, @Param("pattern") String pattern, Pageable pageable);
}

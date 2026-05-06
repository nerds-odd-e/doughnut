package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Notebook;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NotebookRepository extends CrudRepository<Notebook, Integer> {
  List<Notebook> findByOwnership_IdAndDeletedAtIsNull(Integer ownershipId);

  Optional<Notebook> findFirstByNameAndDeletedAtIsNullOrderByIdAsc(String name);

  String notebookNameLike = " WHERE LOWER(nb.name) LIKE LOWER(:pattern) AND nb.deletedAt IS NULL ";
  String notebookNameExact = " WHERE LOWER(nb.name) = LOWER(:key) AND nb.deletedAt IS NULL ";

  @Query("SELECT nb FROM Notebook nb" + notebookNameLike + " AND nb.id = :notebookId")
  List<Notebook> searchInNotebook(
      @Param("notebookId") Integer notebookId, @Param("pattern") String pattern, Pageable pageable);

  @Query("SELECT nb FROM Notebook nb" + notebookNameExact + " AND nb.id = :notebookId")
  List<Notebook> searchExactInNotebook(
      @Param("notebookId") Integer notebookId, @Param("key") String key);

  @Query("SELECT nb FROM Notebook nb" + notebookNameLike + " AND nb.ownership.user.id = :userId")
  List<Notebook> searchForUserInAllMyNotebooks(
      @Param("userId") Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query("SELECT nb FROM Notebook nb" + notebookNameExact + " AND nb.ownership.user.id = :userId")
  List<Notebook> searchExactForUserInAllMyNotebooks(
      @Param("userId") Integer userId, @Param("key") String key);

  @Query(
      "SELECT nb FROM Notebook nb JOIN nb.subscriptions s ON s.user.id = :userId"
          + notebookNameLike)
  List<Notebook> searchForUserInAllMySubscriptions(
      @Param("userId") Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query(
      "SELECT nb FROM Notebook nb JOIN nb.subscriptions s ON s.user.id = :userId"
          + notebookNameExact)
  List<Notebook> searchExactForUserInAllMySubscriptions(
      @Param("userId") Integer userId, @Param("key") String key);

  @Query(
      "SELECT nb FROM Notebook nb JOIN nb.ownership.circle.members m ON m.id = :userId"
          + notebookNameLike)
  List<Notebook> searchForUserInAllMyCircle(
      @Param("userId") Integer userId, @Param("pattern") String pattern, Pageable pageable);

  @Query(
      "SELECT nb FROM Notebook nb JOIN nb.ownership.circle.members m ON m.id = :userId"
          + notebookNameExact)
  List<Notebook> searchExactForUserInAllMyCircle(
      @Param("userId") Integer userId, @Param("key") String key);
}

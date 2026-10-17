package com.odde.donut.entities.repositories;

import com.odde.donut.entities.NotebookGitBinding;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface NotebookGitBindingRepository extends CrudRepository<NotebookGitBinding, Integer> {

  Optional<NotebookGitBinding> findByNotebook_Id(Integer notebookId);

  /** The notebook's accepted Git commit id; null when the notebook has no Git binding. */
  default String findAcceptedRevision(Integer notebookId) {
    return findByNotebook_Id(notebookId)
        .map(NotebookGitBinding::getAcceptedGitObjectId)
        .orElse(null);
  }

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT binding FROM NotebookGitBinding binding WHERE binding.notebook.id = :notebookId")
  Optional<NotebookGitBinding> findByNotebookIdForUpdate(@Param("notebookId") Integer notebookId);
}

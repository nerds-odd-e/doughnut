package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Notebook;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface NotebookRepository extends CrudRepository<Notebook, Integer> {
  List<Notebook> findByOwnership_IdAndDeletedAtIsNull(Integer ownershipId);

  Optional<Notebook> findFirstByPersistedNotebookNameAndDeletedAtIsNullOrderByIdAsc(
      String persistedNotebookName);
}

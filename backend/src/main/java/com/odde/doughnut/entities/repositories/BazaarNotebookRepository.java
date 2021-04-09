package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.BazaarNotebook;
import com.odde.doughnut.entities.NotebookEntity;
import org.springframework.data.repository.CrudRepository;

public interface BazaarNotebookRepository extends CrudRepository<BazaarNotebook, Integer> {
    BazaarNotebook findByNotebookEntity(NotebookEntity notebookEntity);
}

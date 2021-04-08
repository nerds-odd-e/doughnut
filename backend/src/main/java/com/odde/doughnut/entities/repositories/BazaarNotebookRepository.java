package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.BazaarNotebookEntity;
import com.odde.doughnut.entities.NotebookEntity;
import org.springframework.data.repository.CrudRepository;

public interface BazaarNotebookRepository extends CrudRepository<BazaarNotebookEntity, Integer> {
    BazaarNotebookEntity findByNotebookEntity(NotebookEntity notebookEntity);
}

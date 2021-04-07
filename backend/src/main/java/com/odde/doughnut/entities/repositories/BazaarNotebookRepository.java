package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.BazaarNotebookEntity;
import org.springframework.data.repository.CrudRepository;

public interface BazaarNotebookRepository extends CrudRepository<BazaarNotebookEntity, Integer> {
    BazaarNotebookEntity findByNoteId(Integer id);
}

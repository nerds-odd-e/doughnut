package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NotebookEntity;
import org.springframework.data.repository.CrudRepository;

public interface NotebookRepository extends CrudRepository<NotebookEntity, Integer> {
}

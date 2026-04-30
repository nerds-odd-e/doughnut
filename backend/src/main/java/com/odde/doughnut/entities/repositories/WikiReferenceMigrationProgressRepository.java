package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.WikiReferenceMigrationProgress;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface WikiReferenceMigrationProgressRepository
    extends CrudRepository<WikiReferenceMigrationProgress, Integer> {

  Optional<WikiReferenceMigrationProgress> findByStepName(String stepName);
}

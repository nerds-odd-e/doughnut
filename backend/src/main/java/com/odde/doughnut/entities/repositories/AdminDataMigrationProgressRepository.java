package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.AdminDataMigrationProgress;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface AdminDataMigrationProgressRepository
    extends CrudRepository<AdminDataMigrationProgress, Integer> {

  Optional<AdminDataMigrationProgress> findByStepName(String stepName);
}

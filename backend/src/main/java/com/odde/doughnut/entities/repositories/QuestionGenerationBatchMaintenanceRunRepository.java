package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.QuestionGenerationBatchMaintenanceRun;
import com.odde.doughnut.entities.QuestionGenerationBatchMaintenanceTriggerSource;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface QuestionGenerationBatchMaintenanceRunRepository
    extends CrudRepository<QuestionGenerationBatchMaintenanceRun, Integer> {

  Optional<QuestionGenerationBatchMaintenanceRun> findTopByTriggerSourceOrderByStartedAtDesc(
      QuestionGenerationBatchMaintenanceTriggerSource triggerSource);
}

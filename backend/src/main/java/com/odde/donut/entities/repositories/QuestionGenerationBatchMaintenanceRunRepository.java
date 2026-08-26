package com.odde.donut.entities.repositories;

import com.odde.donut.entities.QuestionGenerationBatchMaintenanceRun;
import com.odde.donut.entities.QuestionGenerationBatchMaintenanceTriggerSource;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface QuestionGenerationBatchMaintenanceRunRepository
    extends CrudRepository<QuestionGenerationBatchMaintenanceRun, Integer> {

  Optional<QuestionGenerationBatchMaintenanceRun> findTopByTriggerSourceOrderByStartedAtDesc(
      QuestionGenerationBatchMaintenanceTriggerSource triggerSource);
}

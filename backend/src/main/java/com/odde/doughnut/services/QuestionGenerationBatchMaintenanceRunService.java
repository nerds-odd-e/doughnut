package com.odde.doughnut.services;

import com.odde.doughnut.entities.QuestionGenerationBatchMaintenanceRun;
import com.odde.doughnut.entities.QuestionGenerationBatchMaintenanceTriggerSource;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchMaintenanceRunRepository;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchMaintenanceRunService {
  private final QuestionGenerationBatchMaintenanceRunRepository repository;
  private final ThreadLocal<Integer> currentRunId = new ThreadLocal<>();

  public QuestionGenerationBatchMaintenanceRunService(
      QuestionGenerationBatchMaintenanceRunRepository repository) {
    this.repository = repository;
  }

  public void recordStarted(
      QuestionGenerationBatchMaintenanceTriggerSource triggerSource, Timestamp startedAt) {
    QuestionGenerationBatchMaintenanceRun run = new QuestionGenerationBatchMaintenanceRun();
    run.setTriggerSource(triggerSource);
    run.setStartedAt(startedAt);
    run = repository.save(run);
    currentRunId.set(run.getId());
  }

  public void recordFinished(Timestamp finishedAt) {
    updateCurrentRun(run -> run.setFinishedAt(finishedAt));
    currentRunId.remove();
  }

  public void recordError(RuntimeException e) {
    updateCurrentRun(run -> run.setError(e.getMessage()));
  }

  public Optional<QuestionGenerationBatchMaintenanceRun> findLatestRun(
      QuestionGenerationBatchMaintenanceTriggerSource triggerSource) {
    return repository.findTopByTriggerSourceOrderByStartedAtDesc(triggerSource);
  }

  private void updateCurrentRun(Consumer<QuestionGenerationBatchMaintenanceRun> updater) {
    Integer runId = currentRunId.get();
    if (runId == null) {
      return;
    }
    repository
        .findById(runId)
        .ifPresent(
            run -> {
              updater.accept(run);
              repository.save(run);
            });
  }
}

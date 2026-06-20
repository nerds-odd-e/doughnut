package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.QuestionGenerationBatchAdminStatusDTO;
import com.odde.doughnut.entities.QuestionGenerationBatchMaintenanceTriggerSource;
import com.odde.doughnut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.doughnut.entities.QuestionGenerationBatchStatus;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRepository;
import com.odde.doughnut.entities.repositories.QuestionGenerationBatchRequestRepository;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.stereotype.Service;

@Service
public class QuestionGenerationBatchAdminStatusService {
  private final QuestionGenerationBatchRepository batchRepository;
  private final QuestionGenerationBatchRequestRepository batchRequestRepository;
  private final String openAiToken;
  private final Environment environment;
  private final List<ScheduledTaskHolder> scheduledTaskHolders;
  private final QuestionGenerationBatchMaintenanceRunService maintenanceRunService;

  public QuestionGenerationBatchAdminStatusService(
      QuestionGenerationBatchRepository batchRepository,
      QuestionGenerationBatchRequestRepository batchRequestRepository,
      @Value("${spring.openai.token}") String openAiToken,
      Environment environment,
      List<ScheduledTaskHolder> scheduledTaskHolders,
      QuestionGenerationBatchMaintenanceRunService maintenanceRunService) {
    this.batchRepository = batchRepository;
    this.batchRequestRepository = batchRequestRepository;
    this.openAiToken = openAiToken;
    this.environment = environment;
    this.scheduledTaskHolders = scheduledTaskHolders;
    this.maintenanceRunService = maintenanceRunService;
  }

  public QuestionGenerationBatchAdminStatusDTO getStatus() {
    QuestionGenerationBatchAdminStatusDTO dto = new QuestionGenerationBatchAdminStatusDTO();
    dto.setBatchCountsByStatus(
        toStatusCountMap(batchRepository.countByStatus(), QuestionGenerationBatchStatus.values()));
    dto.setRequestCountsByStatus(
        toStatusCountMap(
            batchRequestRepository.countByStatus(), QuestionGenerationBatchRequestStatus.values()));
    dto.setOpenAiTokenConfigured(openAiToken != null && !openAiToken.isBlank());
    dto.setProdProfileActive(isProdProfileActive());
    dto.setSchedulerActive(isQuestionGenerationBatchMaintenanceScheduled());
    maintenanceRunService
        .findLatestRun(QuestionGenerationBatchMaintenanceTriggerSource.SCHEDULED)
        .ifPresent(
            run -> {
              dto.setLastScheduledMaintenanceStartedAt(run.getStartedAt());
              dto.setLastScheduledMaintenanceFinishedAt(run.getFinishedAt());
              dto.setLastScheduledMaintenanceError(run.getError());
            });
    maintenanceRunService
        .findLatestRun(QuestionGenerationBatchMaintenanceTriggerSource.MANUAL_RESUME)
        .ifPresent(
            run -> {
              dto.setLastManualMaintenanceStartedAt(run.getStartedAt());
              dto.setLastManualMaintenanceFinishedAt(run.getFinishedAt());
              dto.setLastManualMaintenanceError(run.getError());
            });
    return dto;
  }

  private boolean isProdProfileActive() {
    return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equals);
  }

  private boolean isQuestionGenerationBatchMaintenanceScheduled() {
    return scheduledTaskHolders.stream()
        .flatMap(holder -> holder.getScheduledTasks().stream())
        .anyMatch(this::isQuestionGenerationBatchMaintenanceTask);
  }

  private boolean isQuestionGenerationBatchMaintenanceTask(ScheduledTask task) {
    String description = task.toString();
    return description.contains(QuestionGenerationBatchMaintenanceJob.class.getName())
        && description.contains("runHourlyMaintenance");
  }

  private static <E extends Enum<E>> Map<String, Long> toStatusCountMap(
      List<Object[]> rows, E[] allStatuses) {
    Map<String, Long> counts = new LinkedHashMap<>();
    for (E status : allStatuses) {
      counts.put(status.name(), 0L);
    }
    for (Object[] row : rows) {
      @SuppressWarnings("unchecked")
      E status = (E) row[0];
      counts.put(status.name(), (Long) row[1]);
    }
    return counts;
  }
}

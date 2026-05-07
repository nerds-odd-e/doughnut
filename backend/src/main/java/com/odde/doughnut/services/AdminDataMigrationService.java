package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class AdminDataMigrationService implements AdminDataMigrationProgressPopulator {

  public static final String DIAGNOSTIC_MARKER = "admin-data-migration-diagnostics:v1";

  public static final String READY_MESSAGE =
      ("[%s]: Admin-triggered migration runs in bounded batches when steps are configured.")
          .formatted(DIAGNOSTIC_MARKER);

  /**
   * Ordered steps that gate completion reporting and batch routing. Populate this list and
   * implement {@link AdminDataMigrationBatchWorker} when adding migrations.
   */
  public static final List<String> orderedAdminDataMigrationSteps = List.of();

  private final AdminDataMigrationBatchWorker batchWorker;

  public AdminDataMigrationService(@Lazy AdminDataMigrationBatchWorker batchWorker) {
    this.batchWorker = batchWorker;
  }

  public AdminDataMigrationStatusDTO getStatus() {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(READY_MESSAGE);
    populateMigrationProgress(dto);
    return dto;
  }

  public AdminDataMigrationStatusDTO runBatch(User adminUser) {
    return batchWorker.executeBatch(adminUser);
  }

  @Override
  public void populateMigrationProgress(AdminDataMigrationStatusDTO dto) {
    dto.setDataMigrationComplete(true);
    dto.setCurrentStepName(null);
    dto.setStepStatus(WikiReferenceMigrationStepStatus.COMPLETED.name());
    dto.setProcessedCount(0);
    dto.setTotalCount(0);
    dto.setLastError(null);
  }
}

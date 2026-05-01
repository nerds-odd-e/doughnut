package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.User;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Runs one batch for the lowest incomplete step registered in {@link
 * AdminDataMigrationService#orderedAdminDataMigrationSteps}. When none are configured, each call is
 * a no-op acknowledgement so the HTTP surface stays stable.
 *
 * <p>To add migration work later, populate {@link
 * AdminDataMigrationService#orderedAdminDataMigrationSteps} and extend this worker to drive {@link
 * WikiReferenceMigrationProgressService} accordingly.
 */
@Service
public class AdminDataMigrationBatchWorker {

  static final String NO_STEPS_CONFIGURED_BATCH_MESSAGE =
      "Batch acknowledged: no admin data migration steps are configured.";

  private final AdminDataMigrationProgressPopulator progressPopulator;

  public AdminDataMigrationBatchWorker(
      @Lazy AdminDataMigrationProgressPopulator progressPopulator) {
    this.progressPopulator = progressPopulator;
  }

  public AdminDataMigrationStatusDTO executeBatch(@SuppressWarnings("unused") User adminUser) {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(NO_STEPS_CONFIGURED_BATCH_MESSAGE);
    progressPopulator.populateMigrationProgress(dto);
    return dto;
  }
}

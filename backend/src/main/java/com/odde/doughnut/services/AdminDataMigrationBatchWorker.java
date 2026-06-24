package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs one batch for the lowest incomplete step registered in {@link
 * AdminDataMigrationService#orderedAdminDataMigrationSteps}.
 */
@Service
public class AdminDataMigrationBatchWorker {

  static final String TITLE_ALIAS_NO_OP_BATCH_MESSAGE =
      "title_alias_to_frontmatter: batch acknowledged; transform not yet implemented.";

  private final AdminDataMigrationProgressService adminDataMigrationProgressService;
  private final AdminDataMigrationProgressPopulator progressPopulator;

  public AdminDataMigrationBatchWorker(
      AdminDataMigrationProgressService adminDataMigrationProgressService,
      @Lazy AdminDataMigrationProgressPopulator progressPopulator) {
    this.adminDataMigrationProgressService = adminDataMigrationProgressService;
    this.progressPopulator = progressPopulator;
  }

  @Transactional(rollbackFor = Exception.class)
  public AdminDataMigrationStatusDTO executeBatch(@SuppressWarnings("unused") User adminUser) {
    if (AdminDataMigrationService.orderedAdminDataMigrationSteps.isEmpty()) {
      return noStepsConfiguredBatch();
    }
    if (stepCompleted(AdminDataMigrationService.STEP_TITLE_ALIAS_TO_FRONTMATTER)) {
      return migrationAlreadyCompleteBatch();
    }
    return runTitleAliasToFrontmatterNoOpBatch();
  }

  private AdminDataMigrationStatusDTO noStepsConfiguredBatch() {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage("Batch acknowledged: no admin data migration steps are configured.");
    progressPopulator.populateMigrationProgress(dto);
    return dto;
  }

  private AdminDataMigrationStatusDTO migrationAlreadyCompleteBatch() {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage("Title alias to frontmatter migration is already complete.");
    progressPopulator.populateMigrationProgress(dto);
    return dto;
  }

  private AdminDataMigrationStatusDTO runTitleAliasToFrontmatterNoOpBatch() {
    String step = AdminDataMigrationService.STEP_TITLE_ALIAS_TO_FRONTMATTER;
    adminDataMigrationProgressService.startOrResume(step, 0);
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(TITLE_ALIAS_NO_OP_BATCH_MESSAGE);
    progressPopulator.populateMigrationProgress(dto);
    return dto;
  }

  private boolean stepCompleted(String stepName) {
    return adminDataMigrationProgressService
        .find(stepName)
        .map(p -> p.getStatus() == WikiReferenceMigrationStepStatus.COMPLETED)
        .orElse(false);
  }
}

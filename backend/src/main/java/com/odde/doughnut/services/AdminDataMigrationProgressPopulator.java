package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;

/**
 * Fills migration progress onto status DTOs (shared between status API and transactional batch
 * runner).
 */
public interface AdminDataMigrationProgressPopulator {

  void populateMigrationProgress(AdminDataMigrationStatusDTO dto);
}

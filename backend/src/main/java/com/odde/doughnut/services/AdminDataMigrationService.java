package com.odde.doughnut.services;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import org.springframework.stereotype.Service;

@Service
public class AdminDataMigrationService {

  public static final String IDLE_MESSAGE =
      "No data migration pipeline is wired; extend AdminDataMigrationService when needed.";

  public AdminDataMigrationStatusDTO getStatus() {
    AdminDataMigrationStatusDTO dto = new AdminDataMigrationStatusDTO();
    dto.setMessage(IDLE_MESSAGE);
    return dto;
  }

  public AdminDataMigrationStatusDTO runBatch() {
    return getStatus();
  }
}

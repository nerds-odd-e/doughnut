package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.odde.doughnut.controllers.dto.AdminDataMigrationStatusDTO;
import com.odde.doughnut.entities.WikiReferenceMigrationStepStatus;
import com.odde.doughnut.testability.MakeMe;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminDataMigrationServiceTest {

  @Autowired MakeMe makeMe;
  @Autowired AdminDataMigrationService adminDataMigrationService;

  @Test
  void getStatus_isCompleteWhenNoStepsConfigured() {
    AdminDataMigrationStatusDTO dto = adminDataMigrationService.getStatus();

    assertThat(dto.getMessage(), equalTo(AdminDataMigrationService.READY_MESSAGE));
    assertThat(dto.isDataMigrationComplete(), equalTo(true));
    assertThat(dto.getStepStatus(), equalTo(WikiReferenceMigrationStepStatus.COMPLETED.name()));
  }

  @Test
  void runBatch_invokesSkeletonWorker_andMatchesStatusProgress() {
    AdminDataMigrationStatusDTO dto = adminDataMigrationService.runBatch(makeMe.anAdmin().please());

    assertThat(dto.getMessage(), containsString("Batch acknowledged"));
    assertThat(dto.isDataMigrationComplete(), equalTo(true));
    assertThat(dto.getStepStatus(), equalTo(adminDataMigrationService.getStatus().getStepStatus()));
  }
}

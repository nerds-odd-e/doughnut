package com.odde.doughnut.services;

import com.odde.doughnut.entities.NotebookCertificateApproval;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.sql.Timestamp;

public class NotebookCertificateApprovalService {
  private final NotebookCertificateApproval certificateApproval;
  private final ModelFactoryService modelFactoryService;

  public NotebookCertificateApprovalService(
      NotebookCertificateApproval certificateApproval, ModelFactoryService modelFactoryService) {

    this.certificateApproval = certificateApproval;
    this.modelFactoryService = modelFactoryService;
  }

  public NotebookCertificateApproval approve(Timestamp currentUTCTimestamp) {
    certificateApproval.setLastApprovalTime(currentUTCTimestamp);
    modelFactoryService.save(certificateApproval);
    return certificateApproval;
  }

  public NotebookCertificateApproval getApproval() {
    return certificateApproval;
  }
}

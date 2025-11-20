package com.odde.doughnut.services;

import com.odde.doughnut.entities.NotebookCertificateApproval;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;

public class NotebookCertificateApprovalService {
  private final NotebookCertificateApproval certificateApproval;
  private final EntityManager entityManager;

  public NotebookCertificateApprovalService(
      NotebookCertificateApproval certificateApproval, EntityManager entityManager) {
    this.certificateApproval = certificateApproval;
    this.entityManager = entityManager;
  }

  public NotebookCertificateApproval approve(Timestamp currentUTCTimestamp) {
    certificateApproval.setLastApprovalTime(currentUTCTimestamp);
    entityManager.merge(certificateApproval);
    return certificateApproval;
  }

  public NotebookCertificateApproval getApproval() {
    return certificateApproval;
  }
}

package com.odde.doughnut.services;

import com.odde.doughnut.entities.NotebookCertificateApproval;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import org.springframework.stereotype.Service;

@Service
public class NotebookCertificateApprovalService {
  private final EntityPersister entityPersister;

  public NotebookCertificateApprovalService(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
  }

  public NotebookCertificateApproval approve(
      NotebookCertificateApproval certificateApproval, Timestamp currentUTCTimestamp) {
    certificateApproval.setLastApprovalTime(currentUTCTimestamp);
    entityPersister.save(certificateApproval);
    return certificateApproval;
  }
}

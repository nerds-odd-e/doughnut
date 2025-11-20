package com.odde.doughnut.services;

import com.odde.doughnut.entities.NotebookCertificateApproval;
import com.odde.doughnut.entities.repositories.NotebookCertificateApprovalRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotebookCertificateApprovalService {
  private final EntityPersister entityPersister;
  private final NotebookCertificateApprovalRepository notebookCertificateApprovalRepository;

  public NotebookCertificateApprovalService(
      EntityPersister entityPersister,
      NotebookCertificateApprovalRepository notebookCertificateApprovalRepository) {
    this.entityPersister = entityPersister;
    this.notebookCertificateApprovalRepository = notebookCertificateApprovalRepository;
  }

  public NotebookCertificateApproval approve(
      NotebookCertificateApproval certificateApproval, Timestamp currentUTCTimestamp) {
    certificateApproval.setLastApprovalTime(currentUTCTimestamp);
    entityPersister.save(certificateApproval);
    return certificateApproval;
  }

  public List<NotebookCertificateApproval> findByLastApprovalTimeIsNull() {
    return notebookCertificateApprovalRepository.findByLastApprovalTimeIsNull();
  }
}

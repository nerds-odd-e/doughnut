package com.odde.doughnut.services;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookCertificateApproval;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class NotebookService {
  private final EntityManager entityManager;

  public NotebookService(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  public NotebookCertificateApprovalService requestNotebookApproval(Notebook notebook) {
    NotebookCertificateApproval certificateApproval = new NotebookCertificateApproval();
    certificateApproval.setNotebook(notebook);
    entityManager.persist(certificateApproval);
    return new NotebookCertificateApprovalService(certificateApproval, entityManager);
  }
}

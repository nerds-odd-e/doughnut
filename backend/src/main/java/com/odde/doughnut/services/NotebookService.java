package com.odde.doughnut.services;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookCertificateApproval;
import com.odde.doughnut.factoryServices.EntityPersister;
import org.springframework.stereotype.Service;

@Service
public class NotebookService {
  private final EntityPersister entityPersister;

  public NotebookService(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
  }

  public NotebookCertificateApproval requestNotebookApproval(Notebook notebook) {
    NotebookCertificateApproval certificateApproval = new NotebookCertificateApproval();
    certificateApproval.setNotebook(notebook);
    entityPersister.save(certificateApproval);
    return certificateApproval;
  }
}

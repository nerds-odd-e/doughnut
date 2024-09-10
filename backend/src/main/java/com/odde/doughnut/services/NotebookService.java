package com.odde.doughnut.services;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookCertificateApproval;
import com.odde.doughnut.factoryServices.ModelFactoryService;

public class NotebookService {
  private final Notebook notebook;
  private final ModelFactoryService modelFactoryService;

  public NotebookService(Notebook notebook, ModelFactoryService modelFactoryService) {

    this.notebook = notebook;
    this.modelFactoryService = modelFactoryService;
  }

  public NotebookCertificateApprovalService requestNotebookApproval() {
    NotebookCertificateApproval certificateApproval = new NotebookCertificateApproval();
    certificateApproval.setNotebook(notebook);
    modelFactoryService.save(certificateApproval);
    return new NotebookCertificateApprovalService(certificateApproval, modelFactoryService);
  }
}

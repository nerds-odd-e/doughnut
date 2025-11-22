package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.NotebookCertificateApproval;
import lombok.Getter;
import lombok.Setter;

public class NotebookCertificateApprovalDTO {
  @Getter @Setter private NotebookCertificateApproval approval;

  public NotebookCertificateApprovalDTO() {}

  public NotebookCertificateApprovalDTO(NotebookCertificateApproval approval) {
    this.approval = approval;
  }
}

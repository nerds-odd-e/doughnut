package com.odde.doughnut.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notebook_certificate_approval")
public class NotebookCertificateApproval extends EntityIdentifiedByIdOnly {
  @ManyToOne
  @JoinColumn(name = "notebook_id", referencedColumnName = "id")
  @Getter
  private Notebook notebook;

  @Column(name = "last_approval_time")
  @Getter
  @Setter
  private Timestamp lastApprovalTime;
}

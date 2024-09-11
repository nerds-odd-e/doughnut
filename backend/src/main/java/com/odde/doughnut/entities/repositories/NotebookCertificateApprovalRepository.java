package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.NotebookCertificateApproval;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface NotebookCertificateApprovalRepository
    extends CrudRepository<NotebookCertificateApproval, Integer> {
  List<NotebookCertificateApproval> findByLastApprovalTimeIsNull();
}

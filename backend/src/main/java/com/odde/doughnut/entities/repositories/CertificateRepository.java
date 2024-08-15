package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

public interface CertificateRepository extends CrudRepository<Certificate, Integer> {
  Optional<Certificate> findFirstByNotebookAndUserOrderByExpiryDateDesc(
      Notebook notebook, User user);
}

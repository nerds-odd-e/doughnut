package com.odde.doughnut.entities.repositories;

import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import org.springframework.data.repository.CrudRepository;

public interface CertificateRepository extends CrudRepository<Certificate, Integer> {

  Certificate findByUserAndNotebook(User entity, Notebook notebook);
}

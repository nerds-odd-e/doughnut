package com.odde.doughnut.services;

import com.odde.doughnut.entities.Certificate;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.CertificateRepository;
import org.springframework.stereotype.Service;

@Service
public class CertificateService {
  private final CertificateRepository certificateRepository;

  public CertificateService(CertificateRepository certificateRepository) {
    this.certificateRepository = certificateRepository;
  }

  public Certificate findFirstByUserAndNotebook(User user, Notebook notebook) {
    return certificateRepository.findFirstByUserAndNotebook(user, notebook);
  }
}

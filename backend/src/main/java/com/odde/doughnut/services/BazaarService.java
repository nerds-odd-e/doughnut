package com.odde.doughnut.services;

import com.odde.doughnut.entities.BazaarNotebook;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.BazaarNotebookRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BazaarService {
  private final BazaarNotebookRepository bazaarNotebookRepository;
  private final EntityManager entityManager;

  public BazaarService(
      BazaarNotebookRepository bazaarNotebookRepository, EntityManager entityManager) {
    this.bazaarNotebookRepository = bazaarNotebookRepository;
    this.entityManager = entityManager;
  }

  public List<BazaarNotebook> getAllBazaarNotebooks() {
    Iterable<BazaarNotebook> all = bazaarNotebookRepository.findAllNonDeleted();
    List<BazaarNotebook> bazaarNotebooks = new ArrayList<>();
    all.forEach(bazaarNotebooks::add);
    return bazaarNotebooks;
  }

  public void shareNotebook(Notebook notebook) {
    BazaarNotebook bazaarNotebook = new BazaarNotebook();
    bazaarNotebook.setNotebook(notebook);
    entityManager.persist(bazaarNotebook);
  }

  public void removeFromBazaar(BazaarNotebook bazaarNotebook) {
    BazaarNotebook merged = entityManager.merge(bazaarNotebook);
    entityManager.remove(merged);
    entityManager.flush();
  }
}

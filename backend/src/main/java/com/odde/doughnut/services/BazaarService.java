package com.odde.doughnut.services;

import com.odde.doughnut.entities.BazaarNotebook;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.BazaarNotebookRepository;
import com.odde.doughnut.factoryServices.EntityPersister;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BazaarService {
  private final BazaarNotebookRepository bazaarNotebookRepository;
  private final EntityPersister entityPersister;

  public BazaarService(
      BazaarNotebookRepository bazaarNotebookRepository, EntityPersister entityPersister) {
    this.bazaarNotebookRepository = bazaarNotebookRepository;
    this.entityPersister = entityPersister;
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
    entityPersister.save(bazaarNotebook);
  }

  public void removeFromBazaar(BazaarNotebook bazaarNotebook) {
    entityPersister.remove(bazaarNotebook);
  }
}

package com.odde.donut.services;

import com.odde.donut.entities.BazaarNotebook;
import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.repositories.BazaarNotebookRepository;
import com.odde.donut.factoryServices.EntityPersister;
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

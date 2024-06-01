package com.odde.doughnut.models;

import com.odde.doughnut.entities.BazaarNotebook;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.ArrayList;
import java.util.List;

public class BazaarModel {
  private final ModelFactoryService modelFactoryService;

  public BazaarModel(ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  public List<BazaarNotebook> getAllBazaarNotebooks() {
    Iterable<BazaarNotebook> all = modelFactoryService.bazaarNotebookRepository.findAllNonDeleted();
    List<BazaarNotebook> bazaarNotebooks = new ArrayList<>();
    all.forEach(bazaarNotebooks::add);
    return bazaarNotebooks;
  }

  public void shareNotebook(Notebook notebook) {
    BazaarNotebook bazaarNotebook = new BazaarNotebook();
    bazaarNotebook.setNotebook(notebook);
    modelFactoryService.save(bazaarNotebook);
  }

  public void removeFromBazaar(BazaarNotebook notebook) {
    modelFactoryService.remove(notebook);
  }
}

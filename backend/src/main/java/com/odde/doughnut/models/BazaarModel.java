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

  public List<Notebook> getAllNotebooks() {
    Iterable<BazaarNotebook> all = modelFactoryService.bazaarNotebookRepository.findAllNonDeleted();
    List<Notebook> notes = new ArrayList<>();
    all.forEach(bn -> notes.add(bn.getNotebook()));
    return notes;
  }

  public void shareNote(Notebook notebook) {
    BazaarNotebook bazaarNotebook = new BazaarNotebook();
    bazaarNotebook.setNotebook(notebook);
    modelFactoryService.createRecord(bazaarNotebook);
  }
}

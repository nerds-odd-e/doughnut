package com.odde.doughnut.models;

import com.odde.doughnut.entities.BazaarNotebook;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.repositories.BazaarNotebookRepository;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.ArrayList;
import java.util.List;

public class BazaarModel {
  final BazaarNotebookRepository bazaarNotebookRepository;

  public BazaarModel(ModelFactoryService modelFactoryService) {
    bazaarNotebookRepository = modelFactoryService.bazaarNotebookRepository;
  }

  public List<Notebook> getAllNotebooks() {
    Iterable<BazaarNotebook> all = bazaarNotebookRepository.findAllNonDeleted();
    List<Notebook> notes = new ArrayList<>();
    all.forEach(bn -> notes.add(bn.getNotebook()));
    return notes;
  }

  public void shareNote(Notebook notebook) {
    BazaarNotebook bazaarNotebook = new BazaarNotebook();
    bazaarNotebook.setNotebook(notebook);
    bazaarNotebookRepository.save(bazaarNotebook);
  }
}

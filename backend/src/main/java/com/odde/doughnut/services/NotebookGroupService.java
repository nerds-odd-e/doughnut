package com.odde.doughnut.services;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookGroup;
import com.odde.doughnut.entities.Ownership;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import org.springframework.stereotype.Service;

@Service
public class NotebookGroupService {

  private final EntityPersister entityPersister;

  public NotebookGroupService(EntityPersister entityPersister) {
    this.entityPersister = entityPersister;
  }

  public NotebookGroup createGroup(User actor, Ownership ownership, String name)
      throws UnexpectedNoAccessRightException {
    if (!ownership.ownsBy(actor)) {
      throw new UnexpectedNoAccessRightException();
    }
    NotebookGroup group = new NotebookGroup();
    group.setOwnership(ownership);
    group.setName(name);
    entityPersister.save(group);
    entityPersister.refresh(group);
    return group;
  }

  public void assignNotebookToGroup(User actor, Notebook notebook, NotebookGroup group)
      throws UnexpectedNoAccessRightException {
    if (!group.getOwnership().ownsBy(actor)) {
      throw new UnexpectedNoAccessRightException();
    }
    if (!notebook.getOwnership().getId().equals(group.getOwnership().getId())) {
      throw new UnexpectedNoAccessRightException();
    }
    notebook.setNotebookGroup(group);
    entityPersister.save(notebook);
  }

  public void clearNotebookGroup(User actor, Notebook notebook)
      throws UnexpectedNoAccessRightException {
    if (!notebook.getOwnership().ownsBy(actor)) {
      throw new UnexpectedNoAccessRightException();
    }
    notebook.setNotebookGroup(null);
    entityPersister.save(notebook);
  }
}

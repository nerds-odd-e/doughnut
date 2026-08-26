package com.odde.donut.services;

import com.odde.donut.entities.Notebook;
import com.odde.donut.entities.NotebookGroup;
import com.odde.donut.entities.Ownership;
import com.odde.donut.entities.Subscription;
import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.NotebookGroupRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NotebookGroupService {

  private final EntityPersister entityPersister;
  private final NotebookGroupRepository notebookGroupRepository;

  public NotebookGroupService(
      EntityPersister entityPersister, NotebookGroupRepository notebookGroupRepository) {
    this.entityPersister = entityPersister;
    this.notebookGroupRepository = notebookGroupRepository;
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

  public void assignNotebookToGroupById(User actor, Notebook notebook, Integer notebookGroupId)
      throws UnexpectedNoAccessRightException {
    if (notebookGroupId == null) {
      return;
    }
    NotebookGroup group =
        notebookGroupRepository
            .findById(notebookGroupId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    assignNotebookToGroup(actor, notebook, group);
  }

  public void clearNotebookGroup(User actor, Notebook notebook)
      throws UnexpectedNoAccessRightException {
    if (!notebook.getOwnership().ownsBy(actor)) {
      throw new UnexpectedNoAccessRightException();
    }
    notebook.setNotebookGroup(null);
    entityPersister.save(notebook);
  }

  public void assignSubscriptionToGroup(User actor, Subscription subscription, NotebookGroup group)
      throws UnexpectedNoAccessRightException {
    if (!subscription.getUser().equals(actor)) {
      throw new UnexpectedNoAccessRightException();
    }
    if (!group.getOwnership().ownsBy(actor)) {
      throw new UnexpectedNoAccessRightException();
    }
    subscription.setNotebookGroup(group);
    entityPersister.save(subscription);
  }

  public void clearSubscriptionGroup(User actor, Subscription subscription)
      throws UnexpectedNoAccessRightException {
    if (!subscription.getUser().equals(actor)) {
      throw new UnexpectedNoAccessRightException();
    }
    subscription.setNotebookGroup(null);
    entityPersister.save(subscription);
  }
}

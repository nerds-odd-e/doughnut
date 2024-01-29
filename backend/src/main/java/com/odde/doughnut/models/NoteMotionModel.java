package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteMotion;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.factoryServices.ModelFactoryService;

public class NoteMotionModel {
  protected final NoteMotion entity;
  protected final ModelFactoryService modelFactoryService;

  public NoteMotionModel(NoteMotion noteMotion, ModelFactoryService modelFactoryService) {
    this.entity = noteMotion;
    this.modelFactoryService = modelFactoryService;
  }

  public void execute() throws CyclicLinkDetectedException {
    Notebook notebook = entity.getSubject().getNotebook();
    entity.moveHeadNoteOnly();
    Note parent = entity.getNewParent();
    entity.getSubject().setParentNote(parent);
    modelFactoryService.save(entity.getSubject());
    if (notebook.getHeadNote() == entity.getSubject()) {
      modelFactoryService.remove(notebook);
    }
  }
}

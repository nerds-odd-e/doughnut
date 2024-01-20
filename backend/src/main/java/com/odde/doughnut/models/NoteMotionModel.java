package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NoteMotion;
import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import java.util.ArrayList;

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
    updateAncestors(entity.getSubject(), entity.getNewParent());
    modelFactoryService.updateRecord(entity.getSubject());
    modelFactoryService
        .toNoteModel(entity.getSubject())
        .getDescendantsInBreathFirstOrder()
        .forEach(
            desc -> {
              updateAncestors(desc, desc.getParentNote());
              modelFactoryService.updateRecord(desc);
            });
    if (notebook.getHeadNote() == entity.getSubject()) {
      modelFactoryService.notebookRepository.delete(notebook);
    }
  }

  private void updateAncestors(Note note, Note parent) {
    note.getAncestorNotesClosures().forEach(modelFactoryService.notesClosureRepository::delete);
    note.setAncestorNotesClosures(new ArrayList<>());
    modelFactoryService.entityManager.flush();
    note.setParentNote(parent);
  }
}

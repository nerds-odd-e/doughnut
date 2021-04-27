package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.factoryServices.ModelFactoryService;

public class NoteModel {
    protected final Note entity;
    protected final ModelFactoryService modelFactoryService;

    public NoteModel(Note note, ModelFactoryService modelFactoryService) {
        this.entity = note;
        this.modelFactoryService = modelFactoryService;
    }

    public void destroy() {
        entity.traverseBreadthFirst(child ->
                modelFactoryService.toNoteModel(child).destroy());
        modelFactoryService.reviewPointRepository.deleteAllByNote(entity);
        if (entity.getNotebook() != null) {
            if (entity.getNotebook().getHeadNote() == entity) {
                modelFactoryService.notebookRepository.delete(entity.getNotebook());
            }
        }
        modelFactoryService.noteRepository.delete(entity);
    }
}

package com.odde.doughnut.models;

import java.sql.Timestamp;

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
        entity.traverseBreadthFirst(child -> modelFactoryService.toNoteModel(child).destroy());

        Timestamp now = new Timestamp(System.currentTimeMillis());
        
        if (entity.getNotebook() != null) {
            if (entity.getNotebook().getHeadNote() == entity) {        
                entity.getNotebook().setDeletedAt(now);
                modelFactoryService.notebookRepository.save(entity.getNotebook());
            }
        }
        entity.setDeletedAt(now);
        modelFactoryService.noteRepository.save(entity);
    }
}

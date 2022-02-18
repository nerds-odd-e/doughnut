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

    public void destroy(Timestamp currentUTCTimestamp) {
        if (entity.getNotebook() != null) {
            if (entity.getNotebook().getHeadNote() == entity) {
                entity.getNotebook().setDeletedAt(currentUTCTimestamp);
                modelFactoryService.notebookRepository.save(entity.getNotebook());
            }
        }

        entity.setDeletedAt(currentUTCTimestamp);
        modelFactoryService.noteRepository.save(entity);
        modelFactoryService.noteRepository.softDeleteDescendants(entity, currentUTCTimestamp);
    }

    public void restore() {
        if (entity.getNotebook() != null) {
            if (entity.getNotebook().getHeadNote() == entity) {
                entity.getNotebook().setDeletedAt(null);
                modelFactoryService.notebookRepository.save(entity.getNotebook());
            }
        }
        modelFactoryService.noteRepository.undoDeleteDescendants(entity, entity.getDeletedAt());
        entity.setDeletedAt(null);
        modelFactoryService.noteRepository.save(entity);
    }
}

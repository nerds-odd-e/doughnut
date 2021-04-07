package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.ModelFactoryService;

public class TreeNodeModel extends ModelForEntity<NoteEntity> {
    protected final NoteRepository noteRepository;

    public TreeNodeModel(NoteEntity noteEntity, ModelFactoryService modelFactoryService) {
        super(noteEntity, modelFactoryService);
        this.noteRepository = modelFactoryService.noteRepository;
    }

    public void destroy() {
        entity.traverseBreadthFirst(child ->
                modelFactoryService.toTreeNodeModel(child).destroy());
        modelFactoryService.reviewPointRepository.deleteAllByNoteEntity(getEntity());
        if (entity.getNotebookEntity() != null) {
            if (entity.getNotebookEntity().getHeadNoteEntity() == entity) {
                modelFactoryService.notebookRepository.delete(entity.getNotebookEntity());
            }
        }
        modelFactoryService.noteRepository.delete(getEntity());
    }

}

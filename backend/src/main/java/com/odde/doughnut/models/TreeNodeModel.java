package com.odde.doughnut.models;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.ModelFactoryService;

public class TreeNodeModel extends ModelForEntity<Note> {
    protected final NoteRepository noteRepository;

    public TreeNodeModel(Note note, ModelFactoryService modelFactoryService) {
        super(note, modelFactoryService);
        this.noteRepository = modelFactoryService.noteRepository;
    }

    public void destroy() {
        entity.traverseBreadthFirst(child ->
                modelFactoryService.toTreeNodeModel(child).destroy());
        modelFactoryService.reviewPointRepository.deleteAllByNote(getEntity());
        if (entity.getNotebookEntity() != null) {
            if (entity.getNotebookEntity().getHeadNote() == entity) {
                modelFactoryService.notebookRepository.delete(entity.getNotebookEntity());
            }
        }
        modelFactoryService.noteRepository.delete(getEntity());
    }

}

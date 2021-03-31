package com.odde.doughnut.models;

import com.odde.doughnut.algorithms.SiblingOrder;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.ModelFactoryService;

public class TreeNodeModel extends ModelForEntity<NoteEntity> {
    protected final NoteRepository noteRepository;

    public TreeNodeModel(NoteEntity noteEntity, ModelFactoryService modelFactoryService) {
        super(noteEntity, modelFactoryService);
        this.noteRepository = modelFactoryService.noteRepository;
    }

    private long getSiblingOrderToInsertBehindMe() {
        NoteEntity nextSiblingNote = entity.getNextSibling();
        Long relativeToSiblingOrder = entity.getSiblingOrder();
        if (nextSiblingNote == null) {
            return relativeToSiblingOrder + SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
        }
        return (relativeToSiblingOrder + nextSiblingNote.getSiblingOrder()) / 2;
    }

    private Long getSiblingOrderToBecomeMyFirstChild() {
        NoteEntity firstChild = this.entity.getFirstChild();
        if (firstChild != null) {
            return firstChild.getSiblingOrder() - SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
        }
        return null;
    }

    Long theSiblingOrderItTakesToMoveRelativeToMe(boolean asFirstChildOfNote) {
        if (!asFirstChildOfNote) {
            return getSiblingOrderToInsertBehindMe();
        }
        return getSiblingOrderToBecomeMyFirstChild();
    }

    public void destroy() {
        entity.traverseBreadthFirst(child ->
                modelFactoryService.toTreeNodeModel(child).destroy());
        modelFactoryService.reviewPointRepository.deleteAllByNoteEntity(getEntity());
        modelFactoryService.noteRepository.delete(getEntity());
    }

    //
    // This piece of commented code is for demo purpose
    //
//    public int descendantCount() {
//    }
}

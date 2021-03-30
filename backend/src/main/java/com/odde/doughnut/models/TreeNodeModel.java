package com.odde.doughnut.models;

import com.odde.doughnut.algorithms.SiblingOrder;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.ArrayList;
import java.util.List;

public class TreeNodeModel extends ModelForEntity<NoteEntity> {
    protected final NoteRepository noteRepository;

    public TreeNodeModel(NoteEntity noteEntity, ModelFactoryService modelFactoryService) {
        super(noteEntity, modelFactoryService);
        this.noteRepository = modelFactoryService.noteRepository;
    }

    public List<NoteEntity> getAncestorsIncludingMe() {
        if (entity == null) {
            return new ArrayList<>();
        }
        return entity.getAncestorsIncludingMe();
    }

    public NoteEntity getPreviousSiblingNote() {
        if (entity == null) {
            return null;
        }
        return entity.getSiblings().stream()
                .filter(nc->nc.getSiblingOrder() < entity.getSiblingOrder())
                .reduce((f, s)-> s).orElse(null);
    }

    public NoteEntity getNextSiblingNote() {
        return nextSiblingOfNote(entity);
    }

    public NoteEntity getPreviousNote() {
        NoteEntity result = getPreviousSiblingNote();
        if (result == null) {
            return entity.getParentNote1();
        }
        while (true) {
            List<NoteEntity> children = result.getChildren();
            if(children.size() == 0) {
                return result;
            }
            result = children.get(children.size() - 1);
        }
    }

    public NoteEntity getNextNote() {
        NoteEntity firstChild = getFirstChild();
        if (firstChild != null) {
            return firstChild;
        }
        NoteEntity next = entity;
        while (next != null) {
            NoteEntity sibling = nextSiblingOfNote(next);
            if (sibling != null) {
                return sibling;
            }
            next = next.getParentNote1();
        }
        return null;
    }

    private NoteEntity nextSiblingOfNote(NoteEntity note) {
        return note.getSiblings().stream()
                .filter(nc->nc.getSiblingOrder() > note.getSiblingOrder())
                .findFirst().orElse(null);
    }

    public NoteEntity getFirstChild() {
        return entity.getChildren().stream().findFirst().orElse(null);
    }

    private long getSiblingOrderToInsertBehindMe() {
        NoteEntity nextSiblingNote = getNextSiblingNote();
        Long relativeToSiblingOrder = entity.getSiblingOrder();
        if (nextSiblingNote == null) {
            return relativeToSiblingOrder + SiblingOrder.MINIMUM_SIBLING_ORDER_INCREMENT;
        }
        return (relativeToSiblingOrder + nextSiblingNote.getSiblingOrder()) / 2;
    }

    private Long getSiblingOrderToBecomeMyFirstChild() {
        NoteEntity firstChild = getFirstChild();
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

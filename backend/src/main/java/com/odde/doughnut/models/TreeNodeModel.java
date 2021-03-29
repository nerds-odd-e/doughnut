package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class TreeNodeModel extends ModelForEntity<NoteEntity> {
    protected final NoteRepository noteRepository;

    public TreeNodeModel(NoteEntity noteEntity, ModelFactoryService modelFactoryService) {
        super(noteEntity, modelFactoryService);
        this.noteRepository = modelFactoryService.noteRepository;
    }

    public List<NoteEntity> getAncestors() {
        if (entity == null) {
            return new ArrayList<>();
        }
        List<NoteEntity> ancestry = new ArrayList<>();
        NoteEntity e = entity;
        while(e != null) {
            ancestry.add(0, e);
            e = e.getParentNote();
        }
        return ancestry;
    }

    public NoteEntity getPreviousSiblingNote() {
        if (entity == null || entity.getParentNote() == null) {
            return null;
        }
        return noteRepository.findFirstByParentNoteAndSiblingOrderLessThanOrderBySiblingOrderDesc(entity.getParentNote(), entity.getSiblingOrder());
    }

    public NoteEntity getNextSiblingNote() {
        return nextSiblingOfNote(entity);
    }

    public NoteEntity getPreviousNote() {
        NoteEntity result = getPreviousSiblingNote();
        if (result == null) {
            return entity.getParentNote();
        }
        while (true) {
            NoteEntity lastChild = noteRepository.findFirstByParentNoteOrderBySiblingOrderDesc(result);
            if (lastChild == null) {
                return result;
            }
            result = lastChild;
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
            next = next.getParentNote();
        }
        return null;
    }

    private NoteEntity nextSiblingOfNote(NoteEntity note) {
        if (note.getParentNote() == null) {
            return null;
        }
        return noteRepository.findFirstByParentNoteAndSiblingOrderGreaterThanOrderBySiblingOrder(note.getParentNote(), note.getSiblingOrder());
    }

    public NoteEntity getFirstChild() {
        return noteRepository.findFirstByParentNoteOrderBySiblingOrder(entity);
    }

    private long getSiblingOrderToInsertBehindMe() {
        NoteEntity nextSiblingNote = getNextSiblingNote();
        Long relativeToSiblingOrder = entity.getSiblingOrder();
        if (nextSiblingNote == null) {
            return relativeToSiblingOrder + NoteEntity.MINIMUM_SIBLING_ORDER_INCREMENT;
        }
        return (relativeToSiblingOrder + nextSiblingNote.getSiblingOrder()) / 2;
    }

    private Long getSiblingOrderToBecomeMyFirstChild() {
        NoteEntity firstChild = getFirstChild();
        if (firstChild != null) {
            return firstChild.getSiblingOrder() - NoteEntity.MINIMUM_SIBLING_ORDER_INCREMENT;
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
        traverse(child ->
                modelFactoryService.toTreeNodeModel(child).destroy());
        modelFactoryService.reviewPointRepository.deleteAllByNoteEntity(getEntity());
        modelFactoryService.noteRepository.delete(getEntity());
    }

    private void traverse(Consumer<NoteEntity> noteEntityConsumer) {
        getEntity().getChildren().forEach(
                noteEntityConsumer
        );
    }

    public List<NoteEntity> getSiblings() {
        return noteRepository.findAllByParentNote(entity.getParentNote());
    }

    //
    // This piece of commented code is for demo purpose
    //
//    public int descendantCount() {
//    }
}

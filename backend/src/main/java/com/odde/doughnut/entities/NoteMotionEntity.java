package com.odde.doughnut.entities;

import com.odde.doughnut.models.NoteModel;
import com.odde.doughnut.services.ModelFactoryService;
import lombok.Getter;
import lombok.Setter;

public class NoteMotionEntity {
    @Getter @Setter NoteEntity relativeToNote;
    @Getter @Setter private boolean asFirstChildOfNote;

    public NoteMotionEntity(NoteEntity relativeToNote, boolean asFirstChildOfNote) {
        this.relativeToNote = relativeToNote;
        this.asFirstChildOfNote = asFirstChildOfNote;
    }

    public void execute(NoteEntity subject, ModelFactoryService modelFactoryService) {
        subject.setParentNote(getNewParent());
        Long newSiblingOrder = getNewSiblingOrder(modelFactoryService);
        if (newSiblingOrder != null) {
            subject.setSiblingOrder(newSiblingOrder);
        }
        modelFactoryService.noteRepository.save(subject);
    }

    private NoteEntity getNewParent() {
        if (asFirstChildOfNote) {
            return relativeToNote;
        }
        return relativeToNote.getParentNote();
    }

    private Long getNewSiblingOrder(ModelFactoryService modelFactoryService) {
        NoteModel noteModel = modelFactoryService.toNoteModel(relativeToNote);
        if (!asFirstChildOfNote) {
            NoteEntity nextSiblingNote = noteModel.getNextSiblingNote();
            if (nextSiblingNote == null) {
                return relativeToNote.getSiblingOrder() + NoteEntity.MINIMUM_SIBLING_ORDER_INCREMENT;
            }
            return (relativeToNote.getSiblingOrder() + nextSiblingNote.getSiblingOrder()) / 2;
        }
        NoteEntity firstChild = noteModel.getFirstChild();
        if (firstChild != null) {
            return firstChild.getSiblingOrder() - NoteEntity.MINIMUM_SIBLING_ORDER_INCREMENT;
        }
        return null;
    }
}

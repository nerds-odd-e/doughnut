package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.services.ModelFactoryService;
import lombok.Getter;
import lombok.Setter;

public class NoteMotion {
    @Getter @Setter NoteEntity relativeToNote;
    @Getter @Setter private boolean asFirstChildOfNote;

    public NoteMotion(NoteEntity relativeToNote, boolean asFirstChildOfNote) {
        this.relativeToNote = relativeToNote;
        this.asFirstChildOfNote = asFirstChildOfNote;
    }

    public void execute(NoteEntity noteEntity, ModelFactoryService modelFactoryService) {
        NoteModel noteModel = modelFactoryService.toModel(relativeToNote);
        noteEntity.setParentNote(getNewParent());
        noteEntity.setSiblingOrder(getNewSiblingOrder(noteEntity, noteModel));
        modelFactoryService.noteRepository.save(noteEntity);
    }

    private NoteEntity getNewParent() {
        if (asFirstChildOfNote) {
            return relativeToNote;
        }
        return relativeToNote.getParentNote();
    }

    private long getNewSiblingOrder(NoteEntity noteEntity, NoteModel noteModel) {
        if (!asFirstChildOfNote) {
            NoteEntity nextSiblingNote = noteModel.getNextSiblingNote();
            if (nextSiblingNote == null) {
                return relativeToNote.getSiblingOrder() + 1000;
            }
            return (relativeToNote.getSiblingOrder() + nextSiblingNote.getSiblingOrder()) / 2;
        }
        NoteEntity firstChild = noteModel.getFirstChild();
        if (firstChild != null) {
            return firstChild.getSiblingOrder() - 1000;
        }
        return noteEntity.getSiblingOrder();
    }
}

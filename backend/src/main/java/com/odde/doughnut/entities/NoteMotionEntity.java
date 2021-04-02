package com.odde.doughnut.entities;

import com.odde.doughnut.exceptions.CyclicLinkDetectedException;
import lombok.Getter;
import lombok.Setter;

public class NoteMotionEntity {
    @Getter @Setter NoteEntity subject;
    @Getter @Setter NoteEntity relativeToNote;
    @Setter private boolean asFirstChildOfNote;

    public NoteMotionEntity(NoteEntity relativeToNote, boolean asFirstChildOfNote) {
        this.relativeToNote = relativeToNote;
        this.asFirstChildOfNote = asFirstChildOfNote;
    }

    public NoteEntity getNewParent() {
        if (asFirstChildOfNote) {
            return relativeToNote;
        }
        return relativeToNote.getParentNote();
    }

    public void moveHeadNoteOnly() throws CyclicLinkDetectedException {
        if(relativeToNote.getAncestors().contains(subject)) {
            throw new CyclicLinkDetectedException();
        }
        subject.updateSiblingOrder(relativeToNote, asFirstChildOfNote);
    }
}

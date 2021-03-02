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

    public NoteEntity getNewParent() {
        if (asFirstChildOfNote) {
            return relativeToNote;
        }
        return relativeToNote.getParentNote();
    }

}

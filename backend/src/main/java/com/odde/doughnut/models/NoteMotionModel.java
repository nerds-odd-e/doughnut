package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.NoteMotionEntity;

public class NoteMotionModel {
    private final NoteMotionEntity noteMotionEntity;
    private final NoteEntity subject;

    public NoteMotionModel(NoteMotionEntity noteMotionEntity, NoteEntity subject) {
        this.noteMotionEntity = noteMotionEntity;
        this.subject = subject;
    }
}

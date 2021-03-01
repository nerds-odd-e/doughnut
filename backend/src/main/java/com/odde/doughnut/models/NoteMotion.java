package com.odde.doughnut.models;

import com.odde.doughnut.entities.NoteEntity;
import lombok.Getter;
import lombok.Setter;

public class NoteMotion {
    @Getter @Setter NoteEntity note;
    @Getter @Setter NoteEntity behind;

    public NoteMotion(NoteEntity noteEntity, NoteEntity entity) {
        note = noteEntity;
        behind = entity;
    }
}

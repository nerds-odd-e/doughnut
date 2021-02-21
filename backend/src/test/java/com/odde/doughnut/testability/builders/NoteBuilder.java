package com.odde.doughnut.testability.builders;

import com.odde.doughnut.models.Note;

public class NoteBuilder {
    public Note inMemoryPlease() {
        Note note = new Note();
        note.setTitle("title");
        return note;
    }
}

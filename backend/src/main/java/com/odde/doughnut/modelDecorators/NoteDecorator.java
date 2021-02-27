package com.odde.doughnut.modelDecorators;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.repositories.NoteRepository;

import java.util.ArrayList;
import java.util.List;

public class NoteDecorator {
    private final NoteRepository noteRepository;
    private final Note note;

    public NoteDecorator(NoteRepository noteRepository, Note note) {
        this.noteRepository = noteRepository;
        this.note = note;
    }

    public List<Note> getAncestors() {
        if (note == null) {
            return new ArrayList<>();
        }
        return noteRepository.findAncestry(note.getId().longValue());
    }

    public String getTitle() {
        return note.getTitle();
    }
}

package com.odde.doughnut.services;

import com.odde.doughnut.models.NoteModel;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DecoratorService {
    @Autowired
    private final NoteRepository noteRepository;
    public DecoratorService(NoteRepository noteRepository)
    {
        this.noteRepository = noteRepository;
    }

    public NoteModel decorate(Note note) {
        return new NoteModel(noteRepository, note);
    }
}

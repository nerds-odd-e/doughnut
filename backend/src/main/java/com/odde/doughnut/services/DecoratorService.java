package com.odde.doughnut.services;

import com.odde.doughnut.modelDecorators.NoteDecorator;
import com.odde.doughnut.models.Note;
import com.odde.doughnut.repositories.NoteRepository;
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

    public NoteDecorator decorate(Note note) {
        return new NoteDecorator(noteRepository, note);
    }
}

package com.odde.doughnut.services;

import com.odde.doughnut.models.NoteModel;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ModelFactoryService {
    @Autowired
    public final NoteRepository noteRepository;
    public ModelFactoryService(NoteRepository noteRepository)
    {
        this.noteRepository = noteRepository;
    }

    public NoteModel toModel(Note note) {
        return new NoteModel(note, this);
    }
}

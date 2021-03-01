package com.odde.doughnut.services;

import com.odde.doughnut.entities.repositories.BazaarNoteRepository;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.models.BazaarModel;
import com.odde.doughnut.models.NoteModel;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ModelFactoryService {
    @Autowired public final NoteRepository noteRepository;
    @Autowired public final UserRepository userRepository;
    @Autowired public final BazaarNoteRepository bazaarNoteRepository;

    public ModelFactoryService(NoteRepository noteRepository, UserRepository userRepository, BazaarNoteRepository bazaarNoteRepository)
    {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.bazaarNoteRepository = bazaarNoteRepository;
    }

    public NoteModel toModel(Note note) {
        return new NoteModel(note, this);
    }

    public BazaarModel getBazaarModel() {
        return new BazaarModel(this);
    }
}

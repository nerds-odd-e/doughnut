package com.odde.doughnut.models;

import com.odde.doughnut.entities.BazaarNote;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.BazaarNoteRepository;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.ArrayList;
import java.util.List;

public class BazaarModel {
    BazaarNoteRepository bazaarNoteRepository;

    public BazaarModel(ModelFactoryService modelFactoryService) {
        bazaarNoteRepository = modelFactoryService.bazaarNoteRepository;
    }

    public List<Note> getAllNotes() {
        Iterable<BazaarNote> all = bazaarNoteRepository.findAll();
        List<Note> notes = new ArrayList<>();
        all.forEach(bn->notes.add(bn.getNote()));
        return notes;
    }

    public void shareNote(Note note) {
        BazaarNote bazaarNote = new BazaarNote();
        bazaarNote.setNote(note);
        bazaarNoteRepository.save(bazaarNote);
    }
}

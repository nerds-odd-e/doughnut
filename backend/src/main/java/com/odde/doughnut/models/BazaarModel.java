package com.odde.doughnut.models;

import com.odde.doughnut.entities.BazaarNote;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.repositories.BazaarNoteRepository;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.ArrayList;
import java.util.List;

public class BazaarModel {
    BazaarNoteRepository bazaarNoteRepository;

    public BazaarModel(ModelFactoryService modelFactoryService) {
        bazaarNoteRepository = modelFactoryService.bazaarNoteRepository;
    }

    public List<NoteEntity> getAllNotes() {
        Iterable<BazaarNote> all = bazaarNoteRepository.findAll();
        List<NoteEntity> notes = new ArrayList<>();
        all.forEach(bn->notes.add(bn.getNote()));
        return notes;
    }

    public void shareNote(NoteEntity note) {
        BazaarNote bazaarNote = new BazaarNote();
        bazaarNote.setNote(note);
        bazaarNoteRepository.save(bazaarNote);
    }
}

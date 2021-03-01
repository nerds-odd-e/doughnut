package com.odde.doughnut.models;

import com.odde.doughnut.entities.BazaarNoteEntity;
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
        Iterable<BazaarNoteEntity> all = bazaarNoteRepository.findAll();
        List<NoteEntity> notes = new ArrayList<>();
        all.forEach(bn->notes.add(bn.getNote()));
        return notes;
    }

    public void shareNote(NoteEntity note) {
        BazaarNoteEntity bazaarNoteEntity = new BazaarNoteEntity();
        bazaarNoteEntity.setNote(note);
        bazaarNoteRepository.save(bazaarNoteEntity);
    }
}

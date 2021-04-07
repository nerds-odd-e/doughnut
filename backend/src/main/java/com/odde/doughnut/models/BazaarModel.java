package com.odde.doughnut.models;

import com.odde.doughnut.entities.BazaarNotebookEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.entities.repositories.BazaarNotebookRepository;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.ArrayList;
import java.util.List;

public class BazaarModel {
    final BazaarNotebookRepository bazaarNotebookRepository;

    public BazaarModel(ModelFactoryService modelFactoryService) {
        bazaarNotebookRepository = modelFactoryService.bazaarNotebookRepository;
    }

    public List<NoteEntity> getAllNotes() {
        Iterable<BazaarNotebookEntity> all = bazaarNotebookRepository.findAll();
        List<NoteEntity> notes = new ArrayList<>();
        all.forEach(bn->notes.add(bn.getNote()));
        return notes;
    }

    public void shareNote(NoteEntity note) {
        BazaarNotebookEntity bazaarNotebookEntity = new BazaarNotebookEntity();
        bazaarNotebookEntity.setNote(note);
        bazaarNotebookRepository.save(bazaarNotebookEntity);
    }
}

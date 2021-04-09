package com.odde.doughnut.models;

import com.odde.doughnut.entities.BazaarNotebookEntity;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.NotebookEntity;
import com.odde.doughnut.entities.repositories.BazaarNotebookRepository;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.services.ModelFactoryService;

import java.util.ArrayList;
import java.util.List;

public class BazaarModel {
    final BazaarNotebookRepository bazaarNotebookRepository;

    public BazaarModel(ModelFactoryService modelFactoryService) {
        bazaarNotebookRepository = modelFactoryService.bazaarNotebookRepository;
    }

    public List<NotebookEntity> getAllNotebooks() {
        Iterable<BazaarNotebookEntity> all = bazaarNotebookRepository.findAll();
        List<NotebookEntity> notes = new ArrayList<>();
        all.forEach(bn->notes.add(bn.getNotebookEntity()));
        return notes;
    }

    public void shareNote(NotebookEntity notebookEntity) {
        BazaarNotebookEntity bazaarNotebookEntity = new BazaarNotebookEntity();
        bazaarNotebookEntity.setNotebookEntity(notebookEntity);
        bazaarNotebookRepository.save(bazaarNotebookEntity);
    }

    public void assertAuthentication(Note note) throws NoAccessRightException {
        if(bazaarNotebookRepository.findByNotebookEntity(note.getNotebookEntity()) == null) {
            throw new NoAccessRightException();
        }
    }
}

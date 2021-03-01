package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.BazaarNoteEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;

public class BazaarNoteBuilder {
    private final MakeMe makeMe;
    private final BazaarNoteEntity bazaarNoteEntity;

    public BazaarNoteBuilder(MakeMe makeMe, NoteEntity note) {
        this.makeMe = makeMe;
        bazaarNoteEntity = new BazaarNoteEntity();
        bazaarNoteEntity.setNote(note);
    }

    public void please(ModelFactoryService modelFactoryService) {
        modelFactoryService.bazaarNoteRepository.save(bazaarNoteEntity);
    }
}

package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.BazaarNoteEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class BazaarNoteBuilder extends EntityBuilder<BazaarNoteEntity> {

    public BazaarNoteBuilder(MakeMe makeMe, NoteEntity note) {
        super(makeMe, new BazaarNoteEntity());
        entity.setNote(note);
    }

    @Override
    protected void beforeCreate() {

    }
}

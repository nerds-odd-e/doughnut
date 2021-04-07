package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.BazaarNotebookEntity;
import com.odde.doughnut.entities.NoteEntity;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class BazaarNoteBuilder extends EntityBuilder<BazaarNotebookEntity> {

    public BazaarNoteBuilder(MakeMe makeMe, NoteEntity note) {
        super(makeMe, new BazaarNotebookEntity());
        entity.setNote(note);
    }

    @Override
    protected void beforeCreate(boolean needPersist) {

    }
}

package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.BazaarNotebook;
import com.odde.doughnut.entities.NotebookEntity;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class BazaarNotebookBuilder extends EntityBuilder<BazaarNotebook> {

    public BazaarNotebookBuilder(MakeMe makeMe, NotebookEntity notebookEntity) {
        super(makeMe, new BazaarNotebook());
        entity.setNotebookEntity(notebookEntity);
    }

    @Override
    protected void beforeCreate(boolean needPersist) {

    }
}

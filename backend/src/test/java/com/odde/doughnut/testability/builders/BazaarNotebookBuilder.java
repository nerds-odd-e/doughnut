package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.BazaarNotebookEntity;
import com.odde.doughnut.entities.NotebookEntity;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class BazaarNotebookBuilder extends EntityBuilder<BazaarNotebookEntity> {

    public BazaarNotebookBuilder(MakeMe makeMe, NotebookEntity notebookEntity) {
        super(makeMe, new BazaarNotebookEntity());
        entity.setNotebookEntity(notebookEntity);
    }

    @Override
    protected void beforeCreate(boolean needPersist) {

    }
}

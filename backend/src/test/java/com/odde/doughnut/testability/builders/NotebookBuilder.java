package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.NotebookType;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class NotebookBuilder extends EntityBuilder<Notebook> {
    private MakeMe makeMe;

    public NotebookBuilder(MakeMe makeMe) {
        super(makeMe, new Notebook());
        this.makeMe = makeMe;
    }

    public NotebookBuilder withType(NotebookType type) {
        entity.setNotebookType(type);
        return this;
    }

    public NotebookBuilder byUser(UserModel userModel) {
        entity.setCreatorEntity(userModel.getEntity());
        entity.setOwnership(userModel.getEntity().getOwnership());
        return this;
    }

    @Override
    protected void beforeCreate(boolean needPersist) {


    }
}

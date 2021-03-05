package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityAndModelBuilder;
import com.odde.doughnut.testability.MakeMe;

public class UserBuilder extends EntityAndModelBuilder<UserEntity, UserModel> {
    static final TestObjectCounter exIdCounter = new TestObjectCounter(n -> "exid" + n);
    static final TestObjectCounter nameCounter = new TestObjectCounter(n -> "user" + n);

    public UserBuilder(MakeMe makeMe) {
        super(makeMe, new UserEntity(), UserModel.class);
        entity.setExternalIdentifier(exIdCounter.generate());
        entity.setName(nameCounter.generate());
    }

    public UserBuilder with2Notes() {
        entity.getNotes().add(makeMe.aNote().inMemoryPlease());
        entity.getNotes().add(makeMe.aNote().inMemoryPlease());
        return this;
    }
}



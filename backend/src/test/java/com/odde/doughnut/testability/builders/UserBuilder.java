package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;

public class UserBuilder {
    static final TestObjectCounter exIdCounter = new TestObjectCounter(n->"exid" + n);
    static final TestObjectCounter nameCounter = new TestObjectCounter(n->"user" + n);

    private final UserEntity userEntity = new UserEntity();
    private final MakeMe makeMe;


    public UserBuilder(MakeMe makeMe) {
        this.makeMe = makeMe;
        userEntity.setExternalIdentifier(exIdCounter.generate());
        userEntity.setName(nameCounter.generate());
    }

    public UserBuilder with2Notes() {
        userEntity.getNotes().add(makeMe.aNote().inMemoryPlease());
        userEntity.getNotes().add(makeMe.aNote().inMemoryPlease());
        return this;
    }

    public UserEntity inMemoryPlease() {
        return userEntity;
    }

    public UserEntity please() {
        makeMe.modelFactoryService.entityManager.persist(userEntity);
        return userEntity;
    }

    public UserModel toModelPlease() {
        return makeMe.modelFactoryService.toUserModel(please());
    }
}


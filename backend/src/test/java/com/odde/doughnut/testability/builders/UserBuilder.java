package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.MakeMe;

public class UserBuilder {
    static TestObjectCounter exIdCounter = new TestObjectCounter(n->"exid" + n);
    static TestObjectCounter nameCounter = new TestObjectCounter(n->"user" + n);

    private UserEntity userEntity = new UserEntity();
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
        makeMe.modelFactoryService.userRepository.save(userEntity);
        return userEntity;
    }
}


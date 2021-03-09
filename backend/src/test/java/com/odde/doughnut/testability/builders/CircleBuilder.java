package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.CircleEntity;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class CircleBuilder extends EntityBuilder<CircleEntity> {
    static final TestObjectCounter nameCounter = new TestObjectCounter(n->"circle" + n);

    public CircleBuilder(MakeMe makeMe) {
        super(makeMe, new CircleEntity());
        entity.setName(nameCounter.generate());
    }

    @Override
    protected void beforeCreate() {

    }

    public CircleBuilder withMember(UserModel userModel) {
        entity.getMembers().add(userModel.getEntity());
        return this;
    }
}

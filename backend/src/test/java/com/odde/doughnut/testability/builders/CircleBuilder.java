package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.CircleEntity;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class CircleBuilder extends EntityBuilder<CircleEntity> {
    static final TestObjectCounter nameCounter = new TestObjectCounter(n->"circle" + n);

    public CircleBuilder(CircleModel circleModel, MakeMe makeMe) {
        super(makeMe, circleModel == null ? new CircleEntity() : circleModel.getEntity());
        entity.setName(nameCounter.generate());
    }

    @Override
    protected void beforeCreate(boolean needPersist) {

    }

    public CircleBuilder hasMember(UserModel userModel) {
        entity.getMembers().add(userModel.getEntity());
        return this;
    }

    public CircleModel toModelPlease() {
        return makeMe.modelFactoryService.toCircleModel(please());
    }
}

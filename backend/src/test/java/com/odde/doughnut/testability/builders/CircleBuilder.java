package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Circle;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.models.CircleModel;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class CircleBuilder extends EntityBuilder<Circle> {
    static final TestObjectCounter nameCounter = new TestObjectCounter(n->"circle" + n);

    public CircleBuilder(CircleModel circleModel, MakeMe makeMe) {
        super(makeMe, circleModel == null ? new Circle() : circleModel.getEntity());
        entity.setName(nameCounter.generate());
    }

    @Override
    protected void beforeCreate(boolean needPersist) {

    }

    public CircleBuilder hasMember(UserModel userModel) {
        return hasMember(userModel.getEntity());
    }

    public CircleBuilder hasMember(User user) {
        entity.getMembers().add(user);
        return this;
    }

    public CircleModel toModelPlease() {
        return makeMe.modelFactoryService.toCircleModel(please());
    }
}

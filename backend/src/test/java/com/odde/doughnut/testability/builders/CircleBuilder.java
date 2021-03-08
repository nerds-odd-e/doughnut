package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.CircleEntity;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class CircleBuilder extends EntityBuilder<CircleEntity> {
    public CircleBuilder(MakeMe makeMe) {
        super(makeMe, new CircleEntity());
    }

    @Override
    protected void beforeCreate() {

    }
}

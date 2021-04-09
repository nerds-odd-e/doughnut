package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Notebook;
import com.odde.doughnut.entities.SubscriptionEntity;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

public class SubscriptionBuilder extends EntityBuilder<SubscriptionEntity> {
    public SubscriptionBuilder(MakeMe makeMe, SubscriptionEntity entity) {
        super(makeMe, entity);
    }

    @Override
    protected void beforeCreate(boolean needPersist) {
    }

    public SubscriptionBuilder forNotebook(Notebook notebook) {
        entity.setNotebook(notebook);
        return this;
    }

    public SubscriptionBuilder forUser(UserEntity user) {
        entity.setUserEntity(user);
        return this;
    }
}

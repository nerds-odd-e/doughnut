package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.*;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;

import java.sql.Timestamp;

public class CommentReadStatusBuilder extends EntityBuilder<CommentReadStatus> {
    public CommentReadStatusBuilder(CommentReadStatus status, MakeMe makeMe) {
        super(makeMe, status);

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        createdAt(timestamp);
        updatedAt(timestamp);
    }

    @Override
    protected void beforeCreate(boolean needPersist) {

    }

    public CommentReadStatusBuilder comment(Comment comment) {
        entity.setComment(comment);
        return this;
    }

    public CommentReadStatusBuilder user(User user) {
        entity.setUser(user);
        return this;
    }

    public CommentReadStatusBuilder createdAt(Timestamp timestamp) {
        entity.setCreatedAt(timestamp);
        return this;
    }

    public CommentReadStatusBuilder updatedAt(Timestamp timestamp) {
        entity.setUpdatedAt(timestamp);
        return this;
    }
}

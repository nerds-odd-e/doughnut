package com.odde.doughnut.testability.builders;

import com.odde.doughnut.entities.Comment;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.testability.EntityBuilder;
import com.odde.doughnut.testability.MakeMe;
import org.apache.logging.log4j.util.Strings;

import java.sql.Timestamp;

public class CommentBuilder extends EntityBuilder<Comment>  {
    public CommentBuilder(Comment comment, MakeMe makeMe) {
        super(makeMe, comment);

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        createdAt(timestamp);
        updatedAt(timestamp);
    }

    @Override
    protected void beforeCreate(boolean needPersist) {

    }

    public CommentBuilder note(Note note) {
        entity.setNote(note);
        return this;
    }

    public CommentBuilder user(User user) {
        entity.setUser(user);
        return this;
    }

    public CommentBuilder createdAt(Timestamp timestamp) {
        entity.setCreatedAt(timestamp);
        return this;
    }

    public CommentBuilder updatedAt(Timestamp timestamp) {
        entity.setUpdatedAt(timestamp);
        return this;
    }

    public CommentBuilder content(String content) {
        entity.setContent(content);
        return this;
    }
}

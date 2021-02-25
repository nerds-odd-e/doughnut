package com.odde.doughnut.testability;

import com.odde.doughnut.models.User;
import com.odde.doughnut.testability.builders.NoteBuilder;
import com.odde.doughnut.testability.builders.UserBuilder;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;

@Component
public class MakeMe {
    public UserBuilder aUser() {
        return new UserBuilder(this);
    }

    public NoteBuilder aNote() {
        return new NoteBuilder(this);
    }

    public <T> T refresh(EntityManager entityManager, T object) {
        entityManager.refresh(object);
        return object;
    }
}

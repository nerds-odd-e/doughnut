package com.odde.doughnut.testability.builders;

import com.odde.doughnut.models.User;
import com.odde.doughnut.testability.MakeMe;
import org.hibernate.Session;

public class UserBuilder {
    private User user = new User();
    private final Session hibernateSession;
    private final MakeMe makeMe;

    public UserBuilder(Session session, MakeMe makeMe) {
        this.hibernateSession = session;
        this.makeMe = makeMe;
        user.setExternalIdentifier("121212");
        user.setName("Brown");
    }

    public UserBuilder with2Notes() {
        user.getNotes().add(makeMe.aNote().inMemoryPlease());
        user.getNotes().add(makeMe.aNote().inMemoryPlease());
        return this;
    }

    public User please() {
        hibernateSession.save(user);
        return user;
    }
}


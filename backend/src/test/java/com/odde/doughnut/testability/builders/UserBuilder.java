package com.odde.doughnut.testability.builders;

import com.odde.doughnut.models.User;
import com.odde.doughnut.testability.MakeMe;
import org.hibernate.Session;

public class UserBuilder {
    static TestObjectCounter exIdCounter = new TestObjectCounter(n->"exid" + n);
    static TestObjectCounter nameCounter = new TestObjectCounter(n->"user" + n);

    private User user = new User();
    private final Session hibernateSession;
    private final MakeMe makeMe;


    public UserBuilder(Session session, MakeMe makeMe) {
        this.hibernateSession = session;
        this.makeMe = makeMe;
        user.setExternalIdentifier(exIdCounter.generate());
        user.setName(nameCounter.generate());
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

    public User inMemoryPlease() {
        return user;
    }
}


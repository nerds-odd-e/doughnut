package com.odde.doughnut.testability.builders;

import com.odde.doughnut.models.User;
import org.hibernate.Session;

import java.util.function.Consumer;

public class UserBuilder {
    private User user = new User();
    private final Session hibernateSession;

    public UserBuilder(Session session) {
        this.hibernateSession = session;
        user.setExternalIdentifier("121212");
        user.setName("Brown");
    }

    public User please() {
        hibernateSession.save(user);
        return user;
    }

}

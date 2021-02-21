package com.odde.doughnut.testability.builders;

import com.odde.doughnut.models.User;
import org.hibernate.Session;

public class UserBuilder {
    private User user = new User();

    public UserBuilder() {
        user.setExternalIdentifier("121212");
        user.setName("Brown");
    }

    public User please(Session session) {
        session.save(user);
        return user;
    }
}

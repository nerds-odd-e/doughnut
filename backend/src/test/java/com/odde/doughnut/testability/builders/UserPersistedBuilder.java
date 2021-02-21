package com.odde.doughnut.testability.builders;

import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.UserRepository;
import org.hibernate.Session;

public class UserPersistedBuilder {
    private UserRepository userRepository;
    private User user = new User();

    public UserPersistedBuilder(UserRepository userRepository) {
        this.userRepository = userRepository;
        user.setExternalIdentifier("121212");
        user.setName("Brown");
    }

    public User please() {
       userRepository.save(user);
        return user;
    }

    public User please(Session session) {
        session.save(user);
        return user;
    }
}

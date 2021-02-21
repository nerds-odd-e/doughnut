package com.odde.doughnut.testability.builders;

import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.UserRepository;

public class UserPersistedBuilder {
    private UserRepository userRepository;

    public UserPersistedBuilder(UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    public User please() {
        User user = new User();
        user.setExternalIdentifier("121212");
        user.setName("Brown");
        userRepository.save(user);
        return user;
    }
}

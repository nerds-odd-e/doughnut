package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.models.User;

public class TestCurrentUser implements CurrentUser {
    private final User user;

    public TestCurrentUser(User user) {
        this.user = user;
    }

    @Override
    public User getUser() {
        return user;
    }
}

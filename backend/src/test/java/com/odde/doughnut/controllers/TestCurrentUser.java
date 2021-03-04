package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.models.UserModel;

public class TestCurrentUser implements CurrentUser {
    private final UserModel userModel;

    public TestCurrentUser(UserModel userModel) {
        this.userModel = userModel;
    }

    @Override
    public UserModel getUser() {
        return userModel;
    }
}

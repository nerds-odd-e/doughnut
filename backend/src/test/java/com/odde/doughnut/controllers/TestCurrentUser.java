package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.entities.UserEntity;

public class TestCurrentUser implements CurrentUser {
    private final UserEntity userEntity;

    public TestCurrentUser(UserEntity userEntity) {
        this.userEntity = userEntity;
    }

    @Override
    public UserEntity getUser() {
        return userEntity;
    }
}

package com.odde.doughnut.controllers;

import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

@Component
@RequestScope
public class CurrentUserFromRequest implements CurrentUser {
    @Autowired
    UserRepository userRepository;
    String externalId = null;

    public CurrentUserFromRequest(HttpServletRequest request) {
        Principal userPrincipal = request.getUserPrincipal();
        if (userPrincipal != null) {
            externalId = userPrincipal.getName();
        }
    }

    @Override
    public User getUser() {
        return userRepository.findByExternalIdentifier(externalId);
    }
}

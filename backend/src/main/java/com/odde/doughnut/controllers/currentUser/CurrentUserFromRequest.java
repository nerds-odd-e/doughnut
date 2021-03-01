package com.odde.doughnut.controllers.currentUser;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.UserRepository;
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
    User user = null;

    public CurrentUserFromRequest(HttpServletRequest request) {
        Principal userPrincipal = request.getUserPrincipal();
        if (userPrincipal != null) {
            externalId = userPrincipal.getName();
        }
        request.setAttribute("currentUser", this);
    }

    @Override
    public User getUser() {
        if (user == null && externalId != null) {
            user = userRepository.findByExternalIdentifier(externalId);
        }
        return user;
    }
}

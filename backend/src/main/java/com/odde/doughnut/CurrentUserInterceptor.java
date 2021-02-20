package com.odde.doughnut;

import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;

@Component
public class CurrentUserInterceptor implements HandlerInterceptor {

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        Principal userPrincipal = request.getUserPrincipal();
        if (userPrincipal != null) {
            String name = userPrincipal.getName();
            User user = userRepository.findByExternalIdentifier(name);
            request.setAttribute("currentUser", user);
        }
        return true;
    }
}

package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;

@RestController
@RequestMapping("/api/user")
class RestUserController {
    private final ModelFactoryService modelFactoryService;
    private final CurrentUserFetcher currentUserFetcher;

  public RestUserController(ModelFactoryService modelFactoryService, CurrentUserFetcher currentUserFetcher) {
      this.modelFactoryService = modelFactoryService;
      this.currentUserFetcher = currentUserFetcher;
  }

    @PostMapping("")
    public User createUser(Principal principal, User user, HttpServletRequest request, HttpServletResponse resp, Authentication auth) throws ServletException, IOException {
        user.setExternalIdentifier(principal.getName());
        modelFactoryService.userRepository.save(user);
        return user;
    }

}

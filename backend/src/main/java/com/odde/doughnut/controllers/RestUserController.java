
package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUserFetcher;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.NoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
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

    @GetMapping("")
    public User getUserProfile() {
      return currentUserFetcher.getUser().getEntity();
    }

    @PatchMapping("/{user}")
    public @Valid User updateUser(@Valid User user) throws NoAccessRightException {
        currentUserFetcher.getUser().getAuthorization().assertAuthorization(user);
        modelFactoryService.userRepository.save(user);
        return user;
    }

    static class CurrentUserInfo {
        public User user;
        public String externalIdentifier;
    }

    @GetMapping("/current-user-info")
    public CurrentUserInfo currentUserInfo() {
        CurrentUserInfo currentUserInfo = new CurrentUserInfo();
        currentUserInfo.user = currentUserFetcher.getUser().getEntity();
        currentUserInfo.externalIdentifier = currentUserFetcher.getExternalIdentifier();
        return currentUserInfo;
    }
}

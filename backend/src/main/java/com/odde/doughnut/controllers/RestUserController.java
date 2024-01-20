package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Authorization;
import com.odde.doughnut.models.UserModel;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
class RestUserController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;

  public RestUserController(ModelFactoryService modelFactoryService, UserModel currentUser) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
  }

  @PostMapping("")
  @Transactional
  public User createUser(Principal principal, User user) {
    if (principal == null) Authorization.throwUserNotFound();
    user.setExternalIdentifier(principal.getName());
    modelFactoryService.createRecord(user);
    return user;
  }

  @GetMapping("")
  public User getUserProfile() {
    return currentUser.getEntity();
  }

  @PatchMapping("/{user}")
  @Transactional
  public @Valid User updateUser(@Valid User user) throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(user);
    modelFactoryService.updateRecord(user);
    return user;
  }
}

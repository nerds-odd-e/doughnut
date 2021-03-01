package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.services.ModelFactoryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.view.RedirectView;

import java.security.Principal;

@Controller
public class UserController {
  private final ModelFactoryService modelFactoryService;

  public UserController(ModelFactoryService modelFactoryService) {
    this.modelFactoryService = modelFactoryService;
  }

  @PostMapping("/users")
  public RedirectView createUser(Principal principal, UserEntity userEntity) {
    userEntity.setExternalIdentifier(principal.getName());
    modelFactoryService.userRepository.save(userEntity);
    return new RedirectView("/");
  }
}

package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.UserDTO;
import com.odde.doughnut.entities.Token;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Authorization;
import com.odde.doughnut.models.UserModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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
  public User createUser(Principal principal, @RequestBody User user) {
    if (principal == null) Authorization.throwUserNotFound();
    user.setExternalIdentifier(principal.getName());
    modelFactoryService.save(user);
    return user;
  }

  @GetMapping("")
  public User getUserProfile() {
    return currentUser.getEntity();
  }

  @PatchMapping("/{user}")
  @Transactional
  public User updateUser(
      @PathVariable @Schema(type = "integer") User user, @Valid @RequestBody UserDTO updates)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(user);
    user.setName(updates.getName());
    user.setSpaceIntervals(updates.getSpaceIntervals());
    user.setDailyAssimilationCount(updates.getDailyAssimilationCount());
    modelFactoryService.save(user);
    return user;
  }

  @PostMapping("/{user}/token")
  @Transactional
  public Token createUserToken(@PathVariable @Schema(type = "integer") User user)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(user);
    // TODO: Implement actual token generation and storage
    return new Token("generated_token");
  }

  @DeleteMapping("/{user}/token")
  @Transactional
  public void deleteUserToken(@PathVariable @Schema(type = "integer") User user)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(user);
    // TODO: Implement token deletion
  }

  @GetMapping("/{user}/tokens")
  public List<String> getUserTokens(@PathVariable @Schema(type = "integer") User user)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(user);
    // TODO: Implement fetching user tokens
    return new ArrayList<>();
  }
}

package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.TokenConfigDTO;
import com.odde.doughnut.controllers.dto.UserDTO;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.UserToken;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Authorization;
import com.odde.doughnut.models.UserModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
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

  @PostMapping("/generate-token")
  @Transactional
  public UserToken generateToken(@Valid @RequestBody TokenConfigDTO tokenConfig) {
    currentUser.assertLoggedIn();
    User user = currentUser.getEntity();

    String uuid = UUID.randomUUID().toString();

    // save token to DB
    UserToken userToken = new UserToken(user.getId(), uuid, tokenConfig.getLabel());
    userToken = modelFactoryService.save(userToken);
    return userToken;
  }

  @GetMapping("/get-tokens")
  @Transactional
  public List<UserToken> getTokens() {
    currentUser.assertLoggedIn();
    User user = currentUser.getEntity();
    return modelFactoryService.findTokensByUser(user.getId()).orElse(List.of());
  }
}

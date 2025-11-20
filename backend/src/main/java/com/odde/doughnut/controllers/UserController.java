package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.TokenConfigDTO;
import com.odde.doughnut.controllers.dto.UserDTO;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.UserToken;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.EntityPersister;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.UserService;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
class UserController {
  private final ModelFactoryService modelFactoryService;
  private final EntityPersister entityPersister;
  private final TestabilitySettings testabilitySettings;
  private final AuthorizationService authorizationService;
  private final UserService userService;

  public UserController(
      ModelFactoryService modelFactoryService,
      EntityPersister entityPersister,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService,
      UserService userService) {
    this.modelFactoryService = modelFactoryService;
    this.entityPersister = entityPersister;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
    this.userService = userService;
  }

  @PostMapping("")
  @Transactional
  public User createUser(Principal principal, @RequestBody User user) {
    if (principal == null) AuthorizationService.throwUserNotFound();
    user.setExternalIdentifier(principal.getName());
    entityPersister.save(user);
    return user;
  }

  @GetMapping("")
  public User getUserProfile() {
    return authorizationService.getCurrentUser();
  }

  @PatchMapping("/{user}")
  @Transactional
  public User updateUser(
      @PathVariable @Schema(type = "integer") User user, @Valid @RequestBody UserDTO updates)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(user);
    user.setName(updates.getName());
    user.setSpaceIntervals(updates.getSpaceIntervals());
    user.setDailyAssimilationCount(updates.getDailyAssimilationCount());
    entityPersister.save(user);
    return user;
  }

  @PostMapping("/generate-token")
  @Transactional
  public UserToken generateToken(@Valid @RequestBody TokenConfigDTO tokenConfig) {
    authorizationService.assertLoggedIn();
    User user = authorizationService.getCurrentUser();
    String uuid = UUID.randomUUID().toString();
    UserToken userToken = new UserToken(user.getId(), uuid, tokenConfig.getLabel());
    return entityPersister.save(userToken);
  }

  @GetMapping("/get-tokens")
  @Transactional
  public List<UserToken> getTokens() {
    authorizationService.assertLoggedIn();
    User user = authorizationService.getCurrentUser();
    return userService.findTokensByUser(user.getId()).orElse(List.of());
  }

  @DeleteMapping("/token/{tokenId}")
  public void deleteToken(@PathVariable @Schema(type = "integer") Integer tokenId) {
    authorizationService.assertLoggedIn();
    User user = authorizationService.getCurrentUser();

    Optional<UserToken> userToken = userService.findTokenByTokenId(tokenId);
    if (userToken.isEmpty()) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND, "Token not found");
    }

    if (!userToken.get().getUserId().equals(user.getId())) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN,
          "Token does not belong to the current user");
    }

    userService.deleteToken(tokenId);
  }
}

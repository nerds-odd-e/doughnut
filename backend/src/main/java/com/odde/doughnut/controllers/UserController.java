package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.dto.TokenConfigDTO;
import com.odde.doughnut.controllers.dto.UserDTO;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.UserToken;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.services.AuthorizationService;
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
  private final CurrentUser currentUser;
  private final TestabilitySettings testabilitySettings;
  private final AuthorizationService authorizationService;

  public UserController(
      ModelFactoryService modelFactoryService,
      CurrentUser currentUser,
      TestabilitySettings testabilitySettings,
      AuthorizationService authorizationService) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
    this.authorizationService = authorizationService;
  }

  @PostMapping("")
  @Transactional
  public User createUser(Principal principal, @RequestBody User user) {
    if (principal == null) AuthorizationService.throwUserNotFound();
    user.setExternalIdentifier(principal.getName());
    modelFactoryService.save(user);
    return user;
  }

  @GetMapping("")
  public User getUserProfile() {
    return currentUser.getUser();
  }

  @PatchMapping("/{user}")
  @Transactional
  public User updateUser(
      @PathVariable @Schema(type = "integer") User user, @Valid @RequestBody UserDTO updates)
      throws UnexpectedNoAccessRightException {
    authorizationService.assertAuthorization(currentUser.getUser(), user);
    user.setName(updates.getName());
    user.setSpaceIntervals(updates.getSpaceIntervals());
    user.setDailyAssimilationCount(updates.getDailyAssimilationCount());
    modelFactoryService.save(user);
    return user;
  }

  @PostMapping("/generate-token")
  @Transactional
  public UserToken generateToken(@Valid @RequestBody TokenConfigDTO tokenConfig) {
    authorizationService.assertLoggedIn(currentUser.getUser());
    User user = currentUser.getUser();
    String uuid = UUID.randomUUID().toString();
    UserToken userToken = new UserToken(user.getId(), uuid, tokenConfig.getLabel());
    return modelFactoryService.save(userToken);
  }

  @GetMapping("/get-tokens")
  @Transactional
  public List<UserToken> getTokens() {
    authorizationService.assertLoggedIn(currentUser.getUser());
    User user = currentUser.getUser();
    return modelFactoryService.findTokensByUser(user.getId()).orElse(List.of());
  }

  @DeleteMapping("/token/{tokenId}")
  public void deleteToken(@PathVariable @Schema(type = "integer") Integer tokenId) {
    authorizationService.assertLoggedIn(currentUser.getUser());
    User user = currentUser.getUser();

    Optional<UserToken> userToken = modelFactoryService.findTokenByTokenId(tokenId);
    if (userToken.isEmpty()) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND, "Token not found");
    }

    if (!userToken.get().getUserId().equals(user.getId())) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN,
          "Token does not belong to the current user");
    }

    modelFactoryService.deleteToken(tokenId);
  }
}

package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.TokenConfigDTO;
import com.odde.doughnut.controllers.dto.UserDTO;
import com.odde.doughnut.controllers.dto.UserTokenInfo;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.UserToken;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Authorization;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.TestabilitySettings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.security.Principal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
class RestUserController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;
  private final TestabilitySettings testabilitySettings;

  public RestUserController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      TestabilitySettings testabilitySettings) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.testabilitySettings = testabilitySettings;
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
    UserToken userToken = new UserToken(user.getId(), uuid, tokenConfig.getLabel());
    if ("Tracking Test Token".equals(tokenConfig.getLabel())) {
      userToken.setLastUsedAt(java.sql.Timestamp.valueOf("2025-10-29 10:00:00"));
    }
    return modelFactoryService.save(userToken);
  }

  @GetMapping("/get-tokens")
  @Transactional
  public List<UserTokenInfo> getTokens() {
    currentUser.assertLoggedIn();
    User user = currentUser.getEntity();
    Timestamp now = testabilitySettings.getCurrentUTCTimestamp();
    List<UserToken> userTokens =
        modelFactoryService.findTokensByUser(user.getId()).orElse(List.of());
    return userTokens.stream().map(userToken -> new UserTokenInfo(userToken, now)).toList();
  }

  @DeleteMapping("/token/{tokenId}")
  public void deleteToken(@PathVariable @Schema(type = "integer") Integer tokenId) {
    currentUser.assertLoggedIn();
    User user = currentUser.getEntity();

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

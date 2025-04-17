package com.odde.doughnut.controllers;

import com.odde.doughnut.controllers.dto.UserDTO;
import com.odde.doughnut.controllers.dto.UserTokenDTO;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.UserToken;
import com.odde.doughnut.entities.repositories.UserTokenRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.Authorization;
import com.odde.doughnut.models.UserModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
class RestUserController {
  private final ModelFactoryService modelFactoryService;
  private final UserModel currentUser;
  private final UserTokenRepository userTokenRepository;

  public RestUserController(
      ModelFactoryService modelFactoryService,
      UserModel currentUser,
      UserTokenRepository userTokenRepository) {
    this.modelFactoryService = modelFactoryService;
    this.currentUser = currentUser;
    this.userTokenRepository = userTokenRepository;
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
  public UserTokenDTO createUserToken(@PathVariable @Schema(type = "integer") User user)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(user);

    String token = UUID.randomUUID().toString();
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime expiry = now.plusYears(1);

    UserToken userToken = new UserToken(user, token, now, expiry);
    userTokenRepository.save(userToken);

    return new UserTokenDTO(token, now, expiry);
  }

  @DeleteMapping("/{user}/token")
  @Transactional
  public void deleteUserToken(@PathVariable @Schema(type = "integer") User user)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(user);
    userTokenRepository.deleteAllByUser(user);
  }

  @GetMapping("/{user}/tokens")
  public List<UserTokenDTO> getUserTokens(@PathVariable @Schema(type = "integer") User user)
      throws UnexpectedNoAccessRightException {
    currentUser.assertAuthorization(user);
    List<UserToken> tokens = userTokenRepository.findAllByUser(user);
    return tokens.stream()
        .map(
            token -> new UserTokenDTO(token.getToken(), token.getCreatedAt(), token.getExpiresAt()))
        .collect(Collectors.toList());
  }
}

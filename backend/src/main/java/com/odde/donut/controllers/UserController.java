package com.odde.donut.controllers;

import com.odde.donut.controllers.dto.GeneratedTokenDTO;
import com.odde.donut.controllers.dto.MenuDataDTO;
import com.odde.donut.controllers.dto.QuestionGenerationBatchUserScheduleDTO;
import com.odde.donut.controllers.dto.RecallStatsDTO;
import com.odde.donut.controllers.dto.TokenConfigDTO;
import com.odde.donut.controllers.dto.UserDTO;
import com.odde.donut.entities.User;
import com.odde.donut.entities.UserToken;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.factoryServices.EntityPersister;
import com.odde.donut.services.AssimilationServiceFactory;
import com.odde.donut.services.AuthorizationService;
import com.odde.donut.services.ConversationService;
import com.odde.donut.services.QuestionGenerationBatchPlanningService;
import com.odde.donut.services.RecallService;
import com.odde.donut.services.RecallStatsService;
import com.odde.donut.services.UserService;
import com.odde.donut.testability.TestAccessTokenResolver;
import com.odde.donut.testability.TestabilitySettings;
import com.odde.donut.utils.TimezoneUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/user")
class UserController {
  private final EntityPersister entityPersister;
  private final AuthorizationService authorizationService;
  private final UserService userService;
  private final AssimilationServiceFactory assimilationServiceFactory;
  private final RecallService recallService;
  private final RecallStatsService recallStatsService;
  private final ConversationService conversationService;
  private final QuestionGenerationBatchPlanningService questionGenerationBatchPlanningService;
  private final TestabilitySettings testabilitySettings;
  private final Optional<TestAccessTokenResolver> testAccessTokenResolver;

  @Autowired
  public UserController(
      EntityPersister entityPersister,
      AuthorizationService authorizationService,
      UserService userService,
      AssimilationServiceFactory assimilationServiceFactory,
      RecallService recallService,
      RecallStatsService recallStatsService,
      ConversationService conversationService,
      QuestionGenerationBatchPlanningService questionGenerationBatchPlanningService,
      TestabilitySettings testabilitySettings,
      Optional<TestAccessTokenResolver> testAccessTokenResolver) {
    this.entityPersister = entityPersister;
    this.authorizationService = authorizationService;
    this.userService = userService;
    this.assimilationServiceFactory = assimilationServiceFactory;
    this.recallService = recallService;
    this.recallStatsService = recallStatsService;
    this.conversationService = conversationService;
    this.questionGenerationBatchPlanningService = questionGenerationBatchPlanningService;
    this.testabilitySettings = testabilitySettings;
    this.testAccessTokenResolver = testAccessTokenResolver;
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
    user.setDailyAssimilationCount(updates.getDailyAssimilationCount());
    user.setHealthRemoveEmptyFoldersDefault(
        Objects.requireNonNullElse(updates.getHealthRemoveEmptyFoldersDefault(), false));
    if (updates.getDailyProbeEnabled() != null) {
      user.setDailyProbeEnabled(updates.getDailyProbeEnabled());
    }
    entityPersister.save(user);
    return user;
  }

  @PostMapping("/generate-token")
  @Transactional
  public GeneratedTokenDTO generateToken(@Valid @RequestBody TokenConfigDTO tokenConfig) {
    authorizationService.assertLoggedIn();
    User user = authorizationService.getCurrentUser();
    String uuid = UUID.randomUUID().toString();
    UserToken userToken = new UserToken(user.getId(), uuid, tokenConfig.getLabel());
    entityPersister.save(userToken);
    return new GeneratedTokenDTO(userToken.getId(), uuid, tokenConfig.getLabel());
  }

  @GetMapping("/token-info")
  public UserToken getTokenInfo(HttpServletRequest request) {
    String token = bearerTokenFromRequestOrThrow(request);
    return userService
        .findTokenByToken(token)
        .or(() -> userTokenFromTestAccessToken(token))
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid token"));
  }

  @DeleteMapping("/token-info")
  @Transactional
  public void revokeToken(HttpServletRequest request) {
    UserToken userToken = persistedUserTokenFromBearerOrThrow(request);
    userService.deleteToken(userToken.getId());
  }

  private String bearerTokenFromRequestOrThrow(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization");
    }
    return authHeader.substring(7);
  }

  private UserToken persistedUserTokenFromBearerOrThrow(HttpServletRequest request) {
    String token = bearerTokenFromRequestOrThrow(request);
    return userService
        .findTokenByToken(token)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid token"));
  }

  private Optional<UserToken> userTokenFromTestAccessToken(String token) {
    if (testAccessTokenResolver.isEmpty() || !testAccessTokenResolver.get().handles(token)) {
      return Optional.empty();
    }
    return testAccessTokenResolver.get().resolve(token).map(UserToken::forTestabilityTokenInfo);
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

  @GetMapping("/menu-data")
  @Transactional(readOnly = true)
  public MenuDataDTO getMenuData(@RequestParam(value = "timezone") String timezone) {
    authorizationService.assertLoggedIn();
    User user = authorizationService.getCurrentUser();
    ZoneId timeZone = TimezoneUtils.parseTimezone(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    var assimilationService =
        assimilationServiceFactory.create(user, currentUTCTimestamp, timeZone);
    var assimilationCount = assimilationService.getCounts();
    var recallStatus = recallService.getDueMemoryTrackers(user, currentUTCTimestamp, timeZone, 0);
    var unreadMessages = conversationService.getUnreadMessages(user);

    return new MenuDataDTO(assimilationCount, recallStatus, unreadMessages);
  }

  @GetMapping("/recall-stats")
  @Transactional(readOnly = true)
  public RecallStatsDTO getRecallStats(@RequestParam(value = "timezone") String timezone) {
    authorizationService.assertLoggedIn();
    User user = authorizationService.getCurrentUser();
    ZoneId timeZone = TimezoneUtils.parseTimezone(timezone);
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();
    return recallStatsService.compute(user, timeZone, currentUTCTimestamp);
  }

  @GetMapping("/question-generation-batch-schedule")
  @Transactional(readOnly = true)
  public QuestionGenerationBatchUserScheduleDTO getQuestionGenerationBatchSchedule() {
    authorizationService.assertLoggedIn();
    User user = authorizationService.getCurrentUser();
    Timestamp currentUTCTimestamp = testabilitySettings.getCurrentUTCTimestamp();

    return questionGenerationBatchPlanningService.getNextBatchQuestionSchedule(
        user, currentUTCTimestamp);
  }
}

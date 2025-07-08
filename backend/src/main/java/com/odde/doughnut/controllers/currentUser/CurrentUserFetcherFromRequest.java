package com.odde.doughnut.controllers.currentUser;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class CurrentUserFetcherFromRequest implements CurrentUserFetcher {
  private final UserRepository userRepository;
  private final ModelFactoryService modelFactoryService;
  private String externalId = null;
  private User user = null;

  public CurrentUserFetcherFromRequest(
      HttpServletRequest request,
      UserRepository userRepository,
      ModelFactoryService modelFactoryService) {
    this.userRepository = userRepository;
    this.modelFactoryService = modelFactoryService;
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      user = modelFactoryService.findUserByToken(token).orElse(null);
      if (user != null) {
        externalId = user.getExternalIdentifier();
        return;
      }
    }
    Principal userPrincipal = request.getUserPrincipal();
    if (userPrincipal != null) {
      externalId = userPrincipal.getName();
    }
  }

  @Override
  public UserModel getUser() {
    if (user == null && externalId != null) {
      user = userRepository.findByExternalIdentifier(externalId);
    }
    return modelFactoryService.toUserModel(user);
  }

  @Override
  public String getExternalIdentifier() {
    return externalId;
  }

  @Bean("currentUser")
  @RequestScope
  public UserModel getCurrentUser() {
    return getUser();
  }
}

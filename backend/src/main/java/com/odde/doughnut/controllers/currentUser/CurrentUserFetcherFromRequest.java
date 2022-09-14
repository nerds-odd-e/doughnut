package com.odde.doughnut.controllers.currentUser;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.UserRepository;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import com.odde.doughnut.models.UserModel;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class CurrentUserFetcherFromRequest implements CurrentUserFetcher {
  @Autowired UserRepository userRepository;
  @Autowired ModelFactoryService modelFactoryService;
  String externalId = null;
  User user = null;

  public CurrentUserFetcherFromRequest(HttpServletRequest request) {
    Principal userPrincipal = request.getUserPrincipal();
    if (userPrincipal != null) {
      externalId = userPrincipal.getName();
    }
    request.setAttribute("currentUserFetcher", this);
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
}

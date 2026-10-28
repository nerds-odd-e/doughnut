package com.odde.donut.oauth;

import com.odde.donut.entities.User;
import com.odde.donut.entities.repositories.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.stereotype.Component;

@Component
public record AssistantAccessTokenResolver(
    OAuth2AuthorizationService authorizations,
    AssistantResourcePolicy resourcePolicy,
    UserRepository users) {
  public Optional<Access> resolve(String token) {
    var authorization = authorizations.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
    if (authorization == null || !authorization.getAccessToken().isActive()) {
      return Optional.empty();
    }
    OAuth2AuthorizationRequest request =
        authorization.getAttribute(OAuth2AuthorizationRequest.class.getName());
    var access = authorization.getAccessToken();
    if (request == null
        || !resourcePolicy.permits(
            request.getAdditionalParameters().get("resource"), authorization.getAuthorizedScopes())
        || !resourcePolicy.permits(
            request.getAdditionalParameters().get("resource"), access.getToken().getScopes())
        || !authorization.getAuthorizedScopes().containsAll(access.getToken().getScopes())
        || !List.of(resourcePolicy.endpoints().resource()).equals(access.getClaims().get("aud"))
        || !authorization.getPrincipalName().equals(access.getClaims().get("sub"))) {
      return Optional.empty();
    }
    return Optional.ofNullable(users.findByExternalIdentifier(authorization.getPrincipalName()))
        .map(user -> new Access(user, access.getToken().getScopes()));
  }

  public record Access(User user, Set<String> scopes) {}
}

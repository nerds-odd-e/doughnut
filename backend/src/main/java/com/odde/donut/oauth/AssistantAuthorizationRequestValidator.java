package com.odde.donut.oauth;

import java.util.function.Consumer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator;
import org.springframework.stereotype.Component;

@Component
public record AssistantAuthorizationRequestValidator(AssistantResourcePolicy resourcePolicy)
    implements Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {
  @Override
  public void accept(OAuth2AuthorizationCodeRequestAuthenticationContext context) {
    OAuth2AuthorizationCodeRequestAuthenticationToken request = context.getAuthentication();
    if (!context.getRegisteredClient().getRedirectUris().contains(request.getRedirectUri())) {
      // No protocol response may be sent to an unvalidated callback, including loopback ports.
      var safeRequest =
          new OAuth2AuthorizationCodeRequestAuthenticationToken(
              request.getAuthorizationUri(),
              request.getClientId(),
              (Authentication) request.getPrincipal(),
              null,
              request.getState(),
              request.getScopes(),
              request.getAdditionalParameters());
      throw new OAuth2AuthorizationCodeRequestAuthenticationException(
          new OAuth2Error("invalid_request"), safeRequest);
    }
    OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR.accept(context);
    if (!resourcePolicy.permits(
        request.getAdditionalParameters().get("resource"), request.getScopes())) {
      throw new OAuth2AuthorizationCodeRequestAuthenticationException(
          new OAuth2Error("invalid_target"), request);
    }
    if (!"S256".equals(request.getAdditionalParameters().get("code_challenge_method"))) {
      throw new OAuth2AuthorizationCodeRequestAuthenticationException(
          new OAuth2Error("invalid_request"), request);
    }
  }
}

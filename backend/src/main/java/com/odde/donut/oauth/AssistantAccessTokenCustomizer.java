package com.odde.donut.oauth;

import java.util.Collections;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationGrantAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

@Component
public record AssistantAccessTokenCustomizer(AssistantResourcePolicy resourcePolicy)
    implements OAuth2TokenCustomizer<OAuth2TokenClaimsContext> {
  @Override
  public void customize(OAuth2TokenClaimsContext context) {
    OAuth2AuthorizationGrantAuthenticationToken grant = context.getAuthorizationGrant();
    OAuth2AuthorizationRequest request =
        context.getAuthorization().getAttribute(OAuth2AuthorizationRequest.class.getName());
    if (!resourcePolicy.permits(
            grant.getAdditionalParameters().get("resource"), context.getAuthorizedScopes())
        || !resourcePolicy.permits(
            request.getAdditionalParameters().get("resource"), request.getScopes())) {
      throw new OAuth2AuthenticationException(new OAuth2Error("invalid_target"));
    }
    context.getClaims().audience(Collections.singletonList(resourcePolicy.endpoints().resource()));
  }
}

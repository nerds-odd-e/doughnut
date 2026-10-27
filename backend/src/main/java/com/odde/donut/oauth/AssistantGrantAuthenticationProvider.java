package com.odde.donut.oauth;

import java.util.List;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;

record AssistantGrantAuthenticationProvider(
    AuthenticationProvider delegate, AssistantGrantService grants)
    implements AuthenticationProvider {
  static void serialize(List<AuthenticationProvider> providers, AssistantGrantService grants) {
    providers.replaceAll(provider -> new AssistantGrantAuthenticationProvider(provider, grants));
  }

  @Override
  public Authentication authenticate(Authentication authentication) {
    return grants.authenticateGrant(delegate, authentication);
  }

  @Override
  public boolean supports(Class<?> authentication) {
    return delegate.supports(authentication);
  }
}

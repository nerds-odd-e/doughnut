package com.odde.donut.oauth;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public record AssistantResourcePolicy(AssistantOAuthEndpoints endpoints) {
  public boolean permits(Object resource, Set<String> scopes) {
    return endpoints.resource().equals(resource)
        && (Set.of(AssistantAuthorizationConfiguration.READ_SCOPE).equals(scopes)
            || Set.of(
                    AssistantAuthorizationConfiguration.READ_SCOPE,
                    AssistantAuthorizationConfiguration.RECORD_SCOPE)
                .equals(scopes));
  }
}

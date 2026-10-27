package com.odde.donut.oauth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

@Component
public class AssistantClientBootstrap {
  private final AssistantClientProperties properties;
  private final RegisteredClientRepository clients;

  public AssistantClientBootstrap(
      AssistantClientProperties properties, RegisteredClientRepository clients) {
    this.properties = properties;
    this.clients = clients;
  }

  @EventListener(ApplicationReadyEvent.class)
  @Order(Ordered.LOWEST_PRECEDENCE)
  public void configureClients() {
    properties.getClients().forEach(this::configureClient);
  }

  private void configureClient(AssistantClientProperties.Client configured) {
    RegisteredClient existing = clients.findByClientId(configured.getClientId());
    String id =
        existing == null
            ? UUID.nameUUIDFromBytes(configured.getClientId().getBytes(StandardCharsets.UTF_8))
                .toString()
            : existing.getId();
    RegisteredClient.Builder client =
        RegisteredClient.withId(id)
            .clientId(configured.getClientId())
            .clientName(configured.getClientName())
            .clientSecret(configured.getClientSecret())
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .scope(AssistantAuthorizationConfiguration.READ_SCOPE)
            .clientSettings(
                ClientSettings.builder()
                    .requireProofKey(true)
                    .requireAuthorizationConsent(true)
                    .build())
            .tokenSettings(
                TokenSettings.builder()
                    .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                    .accessTokenTimeToLive(properties.getAccessTokenTimeToLive())
                    .refreshTokenTimeToLive(properties.getRefreshTokenTimeToLive())
                    .reuseRefreshTokens(false)
                    .build());
    if (existing != null) client.clientIdIssuedAt(existing.getClientIdIssuedAt());
    configured.getRedirectUris().forEach(client::redirectUri);
    clients.save(client.build());
  }
}

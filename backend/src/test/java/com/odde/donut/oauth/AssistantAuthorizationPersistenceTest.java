package com.odde.donut.oauth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.odde.donut.entities.User;
import com.odde.donut.testability.SpringTestBase;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

class AssistantAuthorizationPersistenceTest extends SpringTestBase {
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void reloadsClientGrantAndConsentWithTheirUserResourceAndCredentials() {
    User user = makeMe.aUser().please();
    makeMe.entityPersister.flush();
    Instant issuedAt = Instant.parse("2026-10-27T10:00:00Z");
    RegisteredClient client =
        RegisteredClient.withId("hosted-assistant")
            .clientId("assistant-client")
            .clientIdIssuedAt(issuedAt)
            .clientSecret("{noop}test-secret")
            .clientName("Hosted assistant")
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .redirectUri("https://assistant.example/callback")
            .scope("notes:read")
            .clientSettings(
                ClientSettings.builder()
                    .requireProofKey(true)
                    .requireAuthorizationConsent(true)
                    .build())
            .tokenSettings(
                TokenSettings.builder()
                    .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                    .reuseRefreshTokens(false)
                    .build())
            .build();
    JdbcRegisteredClientRepository clients = new JdbcRegisteredClientRepository(jdbcTemplate);
    clients.save(client);
    OAuth2AccessToken accessToken =
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER,
            "access-credential",
            issuedAt,
            issuedAt.plusSeconds(300),
            Set.of("notes:read"));
    OAuth2RefreshToken refreshToken =
        new OAuth2RefreshToken("refresh-credential", issuedAt, issuedAt.plusSeconds(3600));
    OAuth2Authorization authorization =
        OAuth2Authorization.withRegisteredClient(client)
            .principalName(user.getExternalIdentifier())
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of("notes:read"))
            .attribute("resource", "https://donut.example/api/mcp")
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    new JdbcOAuth2AuthorizationService(jdbcTemplate, clients).save(authorization);
    new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, clients)
        .save(
            OAuth2AuthorizationConsent.withId(client.getId(), user.getExternalIdentifier())
                .scope("notes:read")
                .build());

    JdbcRegisteredClientRepository reloadedClients =
        new JdbcRegisteredClientRepository(jdbcTemplate);
    RegisteredClient reloadedClient = reloadedClients.findByClientId(client.getClientId());
    OAuth2Authorization reloadedGrant =
        new JdbcOAuth2AuthorizationService(jdbcTemplate, reloadedClients)
            .findByToken(accessToken.getTokenValue(), OAuth2TokenType.ACCESS_TOKEN);
    OAuth2AuthorizationConsent reloadedConsent =
        new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, reloadedClients)
            .findById(client.getId(), user.getExternalIdentifier());

    assertThat(reloadedClient.getClientName(), equalTo("Hosted assistant"));
    assertThat(reloadedClient.getClientSettings().isRequireProofKey(), equalTo(true));
    assertThat(reloadedClient.getTokenSettings().isReuseRefreshTokens(), equalTo(false));
    assertThat(
        reloadedClient.getTokenSettings().getAccessTokenFormat(),
        equalTo(OAuth2TokenFormat.REFERENCE));
    assertThat(reloadedGrant.getPrincipalName(), equalTo(user.getExternalIdentifier()));
    assertThat(reloadedGrant.getRegisteredClientId(), equalTo(client.getId()));
    assertThat(reloadedGrant.getAuthorizedScopes(), equalTo(Set.of("notes:read")));
    assertThat(
        reloadedGrant.<String>getAttribute("resource"), equalTo("https://donut.example/api/mcp"));
    assertThat(
        reloadedGrant.getAccessToken().getToken().getExpiresAt(),
        equalTo(issuedAt.plusSeconds(300)));
    assertThat(
        reloadedGrant.getRefreshToken().getToken().getTokenValue(), equalTo("refresh-credential"));
    assertThat(reloadedConsent.getScopes(), equalTo(Set.of("notes:read")));
  }
}

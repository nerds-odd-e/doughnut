package com.odde.donut.testability;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.odde.donut.oauth.AssistantClientBootstrap;
import com.odde.donut.services.notebookGit.NotebookGitHistoryService;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Calls the real E2E reset, whose {@code TRUNCATE}s commit implicitly: this test really empties the
 * shared unit-test database. That is safe only while backend tests run sequentially in one JVM and
 * no test depends on another test's committed data; enabling parallel test execution must isolate
 * this class first.
 */
class TestabilityDbResetTest extends SpringTestBase {
  @Autowired TestabilityRestController testabilityRestController;
  @Autowired NotebookGitHistoryService notebookGitHistoryService;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired MockMvc mockMvc;
  @Autowired RegisteredClientRepository clients;
  @Autowired OAuth2AuthorizationService authorizations;
  @Autowired OAuth2AuthorizationConsentService consents;
  @Autowired AssistantClientBootstrap clientBootstrap;

  @AfterTransaction
  void restoreCommittedClientConfiguration() {
    // TRUNCATE commits, but this test's re-seeding is rolled back with its transaction.
    clientBootstrap.configureClients();
  }

  @Test
  void resetLeavesNoNativeGitObjects() {
    notebookGitHistoryService.startHistory(makeMe.aNotebook().please(), Instant.now());

    testabilityRestController.resetDBAndTestabilitySettings();

    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM notebook_git_accepted_object", Long.class),
        equalTo(0L));
  }

  @Test
  void resetRestoresConfiguredClientsWithFreshAuthorizationState() throws Exception {
    RegisteredClient client = clients.findByClientId("test-assistant");
    String subject = makeMe.aUser().please().getExternalIdentifier();
    makeMe.entityPersister.flushAndClear();
    Instant issuedAt = Instant.now();
    OAuth2Authorization grant =
        OAuth2Authorization.withRegisteredClient(client)
            .principalName(subject)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizedScopes(Set.of("notes:read"))
            .accessToken(
                new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "reset-access-credential",
                    issuedAt,
                    issuedAt.plusSeconds(300),
                    Set.of("notes:read")))
            .build();
    authorizations.save(grant);
    consents.save(
        OAuth2AuthorizationConsent.withId(client.getId(), subject).scope("notes:read").build());

    mockMvc
        .perform(post("/api/testability/clean_db_and_reset_testability_settings"))
        .andExpect(status().isOk());

    RegisteredClient restored = clients.findByClientId("test-assistant");
    assertThat(restored.getId(), equalTo(client.getId()));
    assertThat(restored.getClientName(), equalTo("Test assistant"));
    assertThat(
        PasswordEncoderFactories.createDelegatingPasswordEncoder()
            .matches("test-assistant-secret", restored.getClientSecret()),
        equalTo(true));
    assertThat(
        restored.getClientAuthenticationMethods(),
        equalTo(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)));
    assertThat(
        restored.getAuthorizationGrantTypes(),
        equalTo(
            Set.of(
                AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN)));
    assertThat(restored.getRedirectUris(), equalTo(Set.of("https://assistant.example/callback")));
    assertThat(restored.getScopes(), equalTo(Set.of("notes:read", "learning_session:record")));
    assertThat(restored.getClientSettings().isRequireProofKey(), equalTo(true));
    assertThat(restored.getClientSettings().isRequireAuthorizationConsent(), equalTo(true));
    assertThat(
        restored.getTokenSettings().getAccessTokenFormat(), equalTo(OAuth2TokenFormat.REFERENCE));
    assertThat(restored.getTokenSettings().isReuseRefreshTokens(), equalTo(false));
    assertThat(
        clients.findByClientId("another-test-assistant").getClientName(),
        equalTo("Another test assistant"));
    assertThat(authorizations.findById(grant.getId()), nullValue());
    assertThat(consents.findById(client.getId(), subject), nullValue());
  }
}

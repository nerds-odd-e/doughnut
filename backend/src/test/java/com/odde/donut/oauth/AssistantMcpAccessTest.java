package com.odde.donut.oauth;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

class AssistantMcpAccessTest extends AssistantMcpTestSupport {
  @ParameterizedTest
  @ValueSource(strings = {"bearer", "bEaReR"})
  void bearerSchemeIsCaseInsensitiveForAssistantMcpReads(String scheme) throws Exception {
    var note =
        makeMe
            .aNote("Delegated read")
            .notebookOwnedBy(users.findByExternalIdentifier("old_learner"))
            .please();
    var response =
        call(
                issueAccessToken(),
                "read_note",
                Map.of("portablePath", note.getNotebook().getName() + ":" + note.getTitle()),
                scheme)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    var read =
        objectMapper.readTree(
            objectMapper
                .readTree(response.getContentAsString())
                .at("/result/content/0/text")
                .asString());
    assertThat(read.get("title").asString(), is("Delegated read"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"expired", "revoked", "deleted", "resource", "scope", "token-scope", "audience"})
  void invalidDelegationCannotUseAnAccompanyingBrowserSession(String condition) throws Exception {
    String token = issueAccessToken();
    var authorization = authorizations.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
    var update = OAuth2Authorization.from(authorization);
    switch (condition) {
      case "deleted" -> authorizations.remove(authorization);
      case "revoked" -> {
        update.token(
            authorization.getAccessToken().getToken(),
            metadata -> metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, true));
        authorizations.save(update.build());
      }
      case "expired" -> {
        update.token(
            new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                token,
                Instant.now().minusSeconds(20),
                Instant.now().minusSeconds(10),
                Set.of("notes:read")),
            metadata -> metadata.putAll(authorization.getAccessToken().getMetadata()));
        authorizations.save(update.build());
      }
      case "scope" -> authorizations.save(update.authorizedScopes(Set.of("notes:write")).build());
      case "token-scope" -> {
        var access = authorization.getAccessToken().getToken();
        update.token(
            new OAuth2AccessToken(
                access.getTokenType(),
                token,
                access.getIssuedAt(),
                access.getExpiresAt(),
                Set.of("notes:write")),
            metadata -> metadata.putAll(authorization.getAccessToken().getMetadata()));
        authorizations.save(update.build());
      }
      case "resource" -> {
        OAuth2AuthorizationRequest original =
            authorization.getAttribute(OAuth2AuthorizationRequest.class.getName());
        update.attribute(
            OAuth2AuthorizationRequest.class.getName(),
            OAuth2AuthorizationRequest.from(original)
                .additionalParameters(
                    parameters -> parameters.put("resource", "https://foreign.example/api/mcp"))
                .build());
        authorizations.save(update.build());
      }
      case "audience" -> {
        update.token(
            authorization.getAccessToken().getToken(),
            metadata -> {
              var claims = new HashMap<>(authorization.getAccessToken().getClaims());
              claims.put("aud", Collections.singletonList("https://foreign.example/api/mcp"));
              metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, claims);
            });
        authorizations.save(update.build());
      }
    }
    call(token, "list_note_candidates", Map.of("query", "secret"))
        .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest
  @CsvSource({
    "Bearer,/api/user/generate-token",
    "Bearer,/api/user",
    "Bearer,/api/notebooks/create",
    "bearer,/api/user/generate-token",
    "bEaReR,/api/user/generate-token"
  })
  void assistantBearerCannotAuthorizeGeneralApiEvenWithCookies(String scheme, String path)
      throws Exception {
    String token = issueAccessToken();
    mockMvc
        .perform(
            post(path)
                .cookie(session)
                .header(HttpHeaders.AUTHORIZATION, scheme + " " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void generalApiRejectsDeletedCredentialsDespiteBrowserCookies() throws Exception {
    String token = issueAccessToken();
    authorizations.remove(authorizations.findByToken(token, OAuth2TokenType.ACCESS_TOKEN));
    mockMvc
        .perform(
            get("/api/user").cookie(session).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }
}

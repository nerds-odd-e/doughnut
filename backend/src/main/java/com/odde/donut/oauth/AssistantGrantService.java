package com.odde.donut.oauth;

import com.odde.donut.entities.repositories.UserRepository;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationConsentAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenRevocationAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AssistantGrantService {
  private final UserRepository users;
  private final OAuth2AuthorizationService authorizations;
  private final RegisteredClientRepository clients;
  private final JdbcTemplate jdbc;

  public AssistantGrantService(
      UserRepository users,
      OAuth2AuthorizationService authorizations,
      RegisteredClientRepository clients,
      JdbcTemplate jdbc) {
    this.users = users;
    this.authorizations = authorizations;
    this.clients = clients;
    this.jdbc = jdbc;
  }

  @Transactional(
      isolation = Isolation.READ_COMMITTED,
      noRollbackFor = OAuth2AuthenticationException.class)
  public Authentication authenticateGrant(
      AuthenticationProvider delegate, Authentication authentication) {
    String principal = grantPrincipal(authentication);
    if (principal != null) users.findByExternalIdentifierForUpdate(principal);
    // The maintained handler re-reads its authorization after the lock. READ_COMMITTED
    // ensures that a withdrawal which held the lock first is visible to that read.
    return delegate.authenticate(authentication);
  }

  private String grantPrincipal(Authentication authentication) {
    if (authentication instanceof OAuth2AuthorizationCodeRequestAuthenticationToken
        || authentication instanceof OAuth2AuthorizationConsentAuthenticationToken) {
      return ((Authentication) authentication.getPrincipal()).getName();
    }
    String token;
    OAuth2TokenType type;
    if (authentication instanceof OAuth2AuthorizationCodeAuthenticationToken code) {
      token = code.getCode();
      type = new OAuth2TokenType("code");
    } else if (authentication instanceof OAuth2RefreshTokenAuthenticationToken refresh) {
      token = refresh.getRefreshToken();
      type = OAuth2TokenType.REFRESH_TOKEN;
    } else if (authentication instanceof OAuth2TokenRevocationAuthenticationToken revocation) {
      token = revocation.getToken();
      type = null;
    } else {
      return null;
    }
    var authorization = authorizations.findByToken(token, type);
    return authorization == null ? null : authorization.getPrincipalName();
  }

  public List<ConnectedAssistant> connectedAssistants(String principal) {
    return jdbc
        .queryForList(
            "SELECT registered_client_id FROM oauth2_authorization_consent WHERE principal_name = ?"
                + " UNION SELECT registered_client_id FROM oauth2_authorization"
                + " WHERE principal_name = ? AND authorized_scopes = ?",
            String.class,
            principal,
            principal,
            AssistantAuthorizationConfiguration.READ_SCOPE)
        .stream()
        .map(clients::findById)
        .map(
            client ->
                new ConnectedAssistant(
                    client.getClientId(),
                    client.getClientName(),
                    Set.of(AssistantAuthorizationConfiguration.READ_SCOPE)))
        .toList();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void withdraw(String principal, String clientId) {
    users.findByExternalIdentifierForUpdate(principal);
    var client = clients.findByClientId(clientId);
    if (client == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    int grants =
        jdbc.update(
            "DELETE FROM oauth2_authorization WHERE principal_name = ? AND registered_client_id = ?",
            principal,
            client.getId());
    int consent =
        jdbc.update(
            "DELETE FROM oauth2_authorization_consent"
                + " WHERE principal_name = ? AND registered_client_id = ?",
            principal,
            client.getId());
    if (grants + consent == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  }

  public record ConnectedAssistant(String clientId, String clientName, Set<String> scopes) {}
}

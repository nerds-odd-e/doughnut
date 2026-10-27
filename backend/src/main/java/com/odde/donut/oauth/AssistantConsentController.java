package com.odde.donut.oauth;

import com.odde.donut.controllers.currentUser.CurrentUserFetcher;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AssistantConsentController {
  private final OAuth2AuthorizationService authorizations;
  private final RegisteredClientRepository clients;
  private final CurrentUserFetcher currentUserFetcher;
  private final AssistantResourcePolicy resourcePolicy;

  public AssistantConsentController(
      OAuth2AuthorizationService authorizations,
      RegisteredClientRepository clients,
      CurrentUserFetcher currentUserFetcher,
      AssistantResourcePolicy resourcePolicy) {
    this.authorizations = authorizations;
    this.clients = clients;
    this.currentUserFetcher = currentUserFetcher;
    this.resourcePolicy = resourcePolicy;
  }

  @GetMapping(AssistantOAuthEndpoints.CONSENT_CONTEXT)
  public ConsentContext getAssistantConsentContext(
      @RequestParam("client_id") String clientId,
      @RequestParam String state,
      Principal principal,
      HttpServletRequest servletRequest) {
    var pending = authorizations.findByToken(state, new OAuth2TokenType("state"));
    if (pending == null
        || principal == null
        || !pending.getPrincipalName().equals(principal.getName())
        || !principal.getName().equals(currentUserFetcher.getExternalIdentifier())
        || currentUserFetcher.getUser() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid consent request");
    }
    var client = clients.findById(pending.getRegisteredClientId());
    OAuth2AuthorizationRequest request =
        pending.getAttribute(OAuth2AuthorizationRequest.class.getName());
    if (client == null
        || !client.getClientId().equals(clientId)
        || request == null
        || !client.getRedirectUris().contains(request.getRedirectUri())
        || !resourcePolicy.permits(
            request.getAdditionalParameters().get("resource"), request.getScopes())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid consent request");
    }
    CsrfToken csrf = (CsrfToken) servletRequest.getAttribute(CsrfToken.class.getName());
    return new ConsentContext(
        currentUserFetcher.getUser().getName(),
        client.getClientId(),
        client.getClientName(),
        request.getScopes(),
        resourcePolicy.endpoints().resource(),
        request.getRedirectUri(),
        state,
        AssistantOAuthEndpoints.AUTHORIZE,
        csrf.getParameterName(),
        csrf.getToken());
  }

  public record ConsentContext(
      String accountName,
      String clientId,
      String clientName,
      Set<String> scopes,
      String resource,
      String redirectUri,
      String state,
      String formAction,
      String csrfParameterName,
      String csrfToken) {}
}

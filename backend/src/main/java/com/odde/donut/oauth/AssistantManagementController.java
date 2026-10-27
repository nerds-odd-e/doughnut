package com.odde.donut.oauth;

import com.odde.donut.controllers.currentUser.CurrentUserFetcher;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public record AssistantManagementController(
    AssistantGrantService grants, CurrentUserFetcher currentUserFetcher) {
  @GetMapping(AssistantOAuthEndpoints.ASSISTANTS)
  public AssistantManagement getConnectedAssistants(
      Principal principal, HttpServletRequest request) {
    String owner = owner(principal);
    CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    return new AssistantManagement(
        grants.connectedAssistants(owner), csrf.getHeaderName(), csrf.getToken());
  }

  @DeleteMapping(AssistantOAuthEndpoints.ASSISTANT)
  public void withdrawAssistant(@PathVariable String clientId, Principal principal) {
    grants.withdraw(owner(principal), clientId);
  }

  private String owner(Principal principal) {
    if (principal == null
        || currentUserFetcher.getUser() == null
        || !principal.getName().equals(currentUserFetcher.getExternalIdentifier())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    return principal.getName();
  }

  public record AssistantManagement(
      List<AssistantGrantService.ConnectedAssistant> assistants,
      String csrfHeaderName,
      String csrfToken) {}
}

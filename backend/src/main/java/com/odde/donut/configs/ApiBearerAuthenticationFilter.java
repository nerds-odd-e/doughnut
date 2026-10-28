package com.odde.donut.configs;

import com.odde.donut.entities.User;
import com.odde.donut.mcp.McpServerConfiguration;
import com.odde.donut.oauth.AssistantAccessTokenResolver;
import com.odde.donut.oauth.AssistantOAuthEndpoints;
import com.odde.donut.services.UserService;
import com.odde.donut.testability.TestAccessTokenResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.web.filter.OncePerRequestFilter;

/** A bearer credential owns API authentication even when a browser session accompanies it. */
public class ApiBearerAuthenticationFilter extends OncePerRequestFilter {
  public static final String AUTHENTICATED_USER_ATTRIBUTE =
      ApiBearerAuthenticationFilter.class.getName() + ".user";

  public static final String ASSISTANT_SCOPES_ATTRIBUTE =
      ApiBearerAuthenticationFilter.class.getName() + ".assistantScopes";

  private final UserService userService;
  private final AssistantAccessTokenResolver assistantTokens;
  private final AssistantOAuthEndpoints endpoints;
  private final Optional<TestAccessTokenResolver> testTokens;

  public ApiBearerAuthenticationFilter(
      UserService userService,
      AssistantAccessTokenResolver assistantTokens,
      AssistantOAuthEndpoints endpoints,
      Optional<TestAccessTokenResolver> testTokens) {
    this.userService = userService;
    this.assistantTokens = assistantTokens;
    this.endpoints = endpoints;
    this.testTokens = testTokens;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    boolean mcp = request.getRequestURI().equals(McpServerConfiguration.MCP_ENDPOINT);
    String header = request.getHeader("Authorization");
    if (!mcp && (header == null || !header.regionMatches(true, 0, "Bearer", 0, 6))) {
      filterChain.doFilter(request, response);
      return;
    }
    boolean hasBearerToken =
        header != null
            && (mcp
                ? header.regionMatches(true, 0, "Bearer ", 0, 7)
                : header.startsWith("Bearer "));
    String token = hasBearerToken ? header.substring(7) : "";
    if (mcp) token = token.trim();
    Optional<User> user = token.isBlank() ? Optional.empty() : userService.findUserByToken(token);
    if (mcp && user.isEmpty() && !token.isBlank()) {
      var access = assistantTokens.resolve(token);
      user = access.map(AssistantAccessTokenResolver.Access::user);
      access.ifPresent(value -> request.setAttribute(ASSISTANT_SCOPES_ATTRIBUTE, value.scopes()));
    }
    if (!mcp && user.isEmpty() && testTokens.isPresent() && testTokens.get().handles(token)) {
      user = testTokens.get().resolve(token);
    }
    if (user.isEmpty()) {
      if (mcp) {
        response.setHeader(
            "WWW-Authenticate",
            "Bearer resource_metadata=\"" + endpoints.resourceMetadata() + "\"");
      }
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Valid bearer credential required");
      return;
    }
    request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, user.get());
    filterChain.doFilter(request, response);
  }
}

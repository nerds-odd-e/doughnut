package com.odde.donut.mcp;

import com.odde.donut.entities.User;
import com.odde.donut.oauth.AssistantOAuthEndpoints;
import com.odde.donut.services.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Token-only gate for {@code /api/mcp}. Browser cookies / session principals cannot authorize MCP
 * calls; a current personal bearer token is required on every request.
 */
public class McpTokenAuthenticationFilter extends OncePerRequestFilter {
  public static final String AUTHENTICATED_USER_ATTRIBUTE =
      McpTokenAuthenticationFilter.class.getName() + ".user";

  private final UserService userService;
  private final AssistantOAuthEndpoints endpoints;

  public McpTokenAuthenticationFilter(UserService userService, AssistantOAuthEndpoints endpoints) {
    this.userService = userService;
    this.endpoints = endpoints;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path == null || !path.endsWith(McpServerConfiguration.MCP_ENDPOINT);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Optional<User> user = resolveTokenUser(request);
    if (user.isEmpty()) {
      response.setHeader(
          "WWW-Authenticate", "Bearer resource_metadata=\"" + endpoints.resourceMetadata() + "\"");
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Valid personal token required");
      return;
    }
    request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, user.get());
    filterChain.doFilter(request, response);
  }

  private Optional<User> resolveTokenUser(HttpServletRequest request) {
    String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      return Optional.empty();
    }
    String token = authorization.substring("Bearer ".length()).trim();
    if (token.isEmpty()) {
      return Optional.empty();
    }
    return userService.findUserByToken(token);
  }
}

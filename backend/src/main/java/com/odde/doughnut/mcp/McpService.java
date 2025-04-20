package com.odde.doughnut.mcp;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.repositories.UserTokenRepository;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class McpService {
  private final UserTokenRepository userTokenRepository;

  public McpService(UserTokenRepository userTokenRepository) {
    this.userTokenRepository = userTokenRepository;
  }

  @Tool(description = "Get instruction")
  public String getInstruction() {
    return "Doughnut is a Personal Knowledge Management tool";
  }

  @Tool(description = "Get username")
  public String getUsername() {
    User user = getAuthenticatedUser();
    if (user != null) {
      return user.getName();
    }
    return "Unauthenticated User";
  }

  private User getAuthenticatedUser() {
    // Try to get the token from the thread local first
    String token = McpTokenManager.getCurrentToken();

    // If no token but we have users in the map, use the first token we find
    if ((token == null || token.isEmpty()) && McpTokenManager.hasTokens()) {
      token = McpTokenManager.getFirstAvailableToken();
    }

    if (token == null || token.isEmpty()) {
      return null;
    }

    return McpTokenManager.getUser(token);
  }
}

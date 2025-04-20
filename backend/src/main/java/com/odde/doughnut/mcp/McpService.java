package com.odde.doughnut.mcp;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.UserToken;
import com.odde.doughnut.entities.repositories.UserTokenRepository;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class McpService {
  private static final ThreadLocal<String> currentToken = new ThreadLocal<>();
  // Map to store token to user for the SSE session
  private static final Map<String, User> tokenToUserMap = new ConcurrentHashMap<>();

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
    String token = currentToken.get();

    // If no token but we have users in the map, use the first token we find
    if ((token == null || token.isEmpty()) && !tokenToUserMap.isEmpty()) {
      token = tokenToUserMap.keySet().iterator().next();
    }

    if (token == null || token.isEmpty()) {
      return null;
    }

    // First check our local cache
    User user = tokenToUserMap.get(token);
    if (user != null) {
      return user;
    }

    // If not in cache, look it up
    Optional<UserToken> userToken = userTokenRepository.findByToken(token);
    if (userToken.isPresent()) {
      user = userToken.get().getUser();
      // Cache the result
      tokenToUserMap.put(token, user);
      return user;
    } else {
      return null;
    }
  }

  public static void setCurrentToken(String token) {
    currentToken.set(token);
  }

  public static void clearCurrentToken() {
    currentToken.remove();
  }

  // Method to register token-to-user mapping for SSE sessions
  public static void registerTokenUser(String token, User user) {
    if (token != null && !token.isEmpty() && user != null) {
      tokenToUserMap.put(token, user);
    }
  }
}

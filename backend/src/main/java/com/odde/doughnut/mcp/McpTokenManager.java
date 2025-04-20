package com.odde.doughnut.mcp;

import com.odde.doughnut.entities.User;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Manages the token to user mapping across different request contexts for MCP tools. */
public class McpTokenManager {
  private static final ThreadLocal<String> currentToken = new ThreadLocal<>();
  // Map to store token to user for the SSE session
  private static final Map<String, User> tokenToUserMap = new ConcurrentHashMap<>();

  /** Sets the current token in the thread local. */
  public static void setCurrentToken(String token) {
    currentToken.set(token);
  }

  /** Clears the current token from the thread local. */
  public static void clearCurrentToken() {
    currentToken.remove();
  }

  /** Gets the current token from thread local. */
  public static String getCurrentToken() {
    return currentToken.get();
  }

  /** Registers a token to user mapping for SSE sessions. */
  public static void registerTokenUser(String token, User user) {
    setCurrentToken(token);
    if (token != null && !token.isEmpty() && user != null) {
      tokenToUserMap.put(token, user);
    }
  }

  /** Gets a user from the token map. */
  public static User getUser(String token) {
    return tokenToUserMap.get(token);
  }

  /** Gets the first available token if any exists. */
  public static String getFirstAvailableToken() {
    if (!tokenToUserMap.isEmpty()) {
      return tokenToUserMap.keySet().iterator().next();
    }
    return null;
  }

  /** Checks if there are any tokens registered. */
  public static boolean hasTokens() {
    return !tokenToUserMap.isEmpty();
  }

  /** Unregisters a token. */
  public static void unregisterToken(String token) {
    if (token != null && !token.isEmpty()) {
      tokenToUserMap.remove(token);
    }
  }
}

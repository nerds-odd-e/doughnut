package com.odde.doughnut.mcp;

import com.odde.doughnut.entities.User;
import com.odde.doughnut.entities.UserToken;
import com.odde.doughnut.entities.repositories.UserTokenRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class McpTokenFilter implements Filter {
  @Autowired private UserTokenRepository userTokenRepository;

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    try {
      if (request instanceof HttpServletRequest httpRequest) {
        String uri = httpRequest.getRequestURI();

        if (uri.equals("/sse")) {
          String token = httpRequest.getParameter("token");

          if (token != null && !token.isEmpty()) {

            // For SSE sessions, find and register the user in our cache
            findAndRegisterUser(token);
          }
        }
      }
      chain.doFilter(request, response);
    } finally {
      McpTokenManager.clearCurrentToken();
    }
  }

  private void findAndRegisterUser(String token) {
    Optional<UserToken> userToken = userTokenRepository.findByToken(token);
    if (userToken.isPresent()) {
      User user = userToken.get().getUser();
      McpTokenManager.registerTokenUser(token, user);
    }
  }
}

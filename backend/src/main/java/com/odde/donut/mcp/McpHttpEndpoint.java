package com.odde.donut.mcp;

import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adapts the SDK {@link HttpServletStatelessServerTransport} onto {@code /api/mcp} so
 * DispatcherServlet binds request context and MockMvc/production share one path. OpenAPI transport
 * description is owned by {@code SwaggerConfig}.
 */
@Hidden
@RestController
public class McpHttpEndpoint {
  private final HttpServletStatelessServerTransport transport;

  public McpHttpEndpoint(
      HttpServletStatelessServerTransport transport, McpStatelessSyncServer mcpServer) {
    this.transport = transport;
    // Ensure the sync server has registered its handler on the transport before serving.
    mcpServer.getServerInfo();
  }

  @RequestMapping(
      path = McpServerConfiguration.MCP_ENDPOINT,
      method = {RequestMethod.GET, RequestMethod.POST})
  public void handle(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    transport.service(request, response);
  }
}

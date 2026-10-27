package com.odde.donut.mcp;

import com.odde.donut.entities.User;
import com.odde.donut.oauth.AssistantOAuthEndpoints;
import com.odde.donut.services.UserService;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.server.transport.DefaultServerTransportSecurityValidator;
import io.modelcontextprotocol.server.transport.HttpServletStatelessServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.context.request.RequestContextHolder;
import reactor.core.publisher.Mono;

@Configuration
public class McpServerConfiguration {
  public static final String MCP_ENDPOINT = "/api/mcp";
  public static final String TRANSPORT_USER_KEY = "donut.user";

  private final McpFailureReporter mcpFailureReporter;

  public McpServerConfiguration(McpFailureReporter mcpFailureReporter) {
    this.mcpFailureReporter = mcpFailureReporter;
  }

  @Bean
  HttpServletStatelessServerTransport mcpServletTransport() {
    return HttpServletStatelessServerTransport.builder()
        .messageEndpoint(MCP_ENDPOINT)
        .httpHeaderValidator(DefaultServerTransportSecurityValidator.builder().build())
        .contextExtractor(this::extractTransportContext)
        .build();
  }

  @Bean
  McpStatelessSyncServer mcpStatelessSyncServer(
      HttpServletStatelessServerTransport transport,
      ListNoteCandidatesTool listNoteCandidatesTool,
      ReadNoteTool readNoteTool,
      GetNoteGraphTool getNoteGraphTool) {
    McpStatelessServerTransport handlerIntercept =
        new McpStatelessServerTransport() {
          @Override
          public void setMcpHandler(McpStatelessServerHandler handler) {
            transport.setMcpHandler(reportingHandler(handler));
          }

          @Override
          public Mono<Void> closeGracefully() {
            return transport.closeGracefully();
          }
        };

    return McpServer.sync(handlerIntercept)
        .serverInfo("donut", "1.0.0")
        .capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
        .tools(
            listNoteCandidatesTool.specification(),
            readNoteTool.specification(),
            getNoteGraphTool.specification())
        .immediateExecution(true)
        .build();
  }

  @Bean
  FilterRegistrationBean<McpTokenAuthenticationFilter> mcpTokenAuthenticationFilterRegistration(
      UserService userService, AssistantOAuthEndpoints endpoints) {
    FilterRegistrationBean<McpTokenAuthenticationFilter> registration =
        new FilterRegistrationBean<>(new McpTokenAuthenticationFilter(userService, endpoints));
    registration.addUrlPatterns(MCP_ENDPOINT);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
    registration.setName("donutMcpTokenAuthenticationFilter");
    return registration;
  }

  private McpTransportContext extractTransportContext(HttpServletRequest request) {
    if (RequestContextHolder.getRequestAttributes() == null) {
      throw new IllegalStateException("Spring request context must be bound for MCP");
    }
    User user =
        (User) request.getAttribute(McpTokenAuthenticationFilter.AUTHENTICATED_USER_ATTRIBUTE);
    return McpTransportContext.create(Map.of(TRANSPORT_USER_KEY, user));
  }

  private McpStatelessServerHandler reportingHandler(McpStatelessServerHandler delegate) {
    return new McpStatelessServerHandler() {
      @Override
      public Mono<McpSchema.JSONRPCResponse> handleRequest(
          McpTransportContext transportContext, McpSchema.JSONRPCRequest request) {
        return delegate
            .handleRequest(transportContext, request)
            .onErrorResume(
                failure ->
                    Mono.just(
                        McpSchema.JSONRPCResponse.error(
                            request.id(), mcpFailureReporter.toJsonRpcError(failure))));
      }

      @Override
      public Mono<Void> handleNotification(
          McpTransportContext transportContext, McpSchema.JSONRPCNotification notification) {
        return delegate
            .handleNotification(transportContext, notification)
            .onErrorResume(
                failure -> {
                  mcpFailureReporter.reportUnexpected(failure);
                  return Mono.error(failure);
                });
      }
    };
  }
}

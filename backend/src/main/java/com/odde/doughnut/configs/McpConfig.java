package com.odde.doughnut.configs;

import com.odde.doughnut.mcp.McpService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class McpConfig implements WebMvcConfigurer {
  @Bean
  public ToolCallbackProvider instructionTools(McpService mcpService) {
    return MethodToolCallbackProvider.builder().toolObjects(mcpService).build();
  }
}

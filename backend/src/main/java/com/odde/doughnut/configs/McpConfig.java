package com.odde.doughnut.configs;

import com.odde.doughnut.mcp.McpService;
import com.odde.doughnut.mcp.McpTokenFilter;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class McpConfig implements WebMvcConfigurer {

  @Bean
  public ToolCallbackProvider instructionTools(McpService mcpService) {
    return MethodToolCallbackProvider.builder().toolObjects(mcpService).build();
  }

  @Bean
  public FilterRegistrationBean<McpTokenFilter> mcpTokenFilterRegistration(
      McpTokenFilter mcpTokenFilter) {
    FilterRegistrationBean<McpTokenFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(mcpTokenFilter);
    registrationBean.addUrlPatterns("/sse");
    registrationBean.setOrder(1); // Set priority in the filter chain
    return registrationBean;
  }
}

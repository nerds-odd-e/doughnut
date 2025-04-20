package com.odde.doughnut.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class McpService {

  @Tool(description = "Get instruction")
  public String getInstruction() {
    return "Doughnut is a Personal Knowledge Management tool";
  }

  @Tool(description = "Get username")
  public String getUsername() {
    return "Terry";
  }
}

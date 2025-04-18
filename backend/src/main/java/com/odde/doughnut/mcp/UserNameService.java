package com.odde.doughnut.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class UserNameService {

  @Tool(description = "Get username")
  public String getUsername() {
    return "Terry";
  }
}

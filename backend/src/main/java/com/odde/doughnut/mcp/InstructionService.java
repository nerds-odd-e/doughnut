package com.odde.doughnut.mcp;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class InstructionService {

  @Tool(description = "Get instruction")
  public String getInstruction() {
    return "hello";
  }
}

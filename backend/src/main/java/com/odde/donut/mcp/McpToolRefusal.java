package com.odde.donut.mcp;

import io.modelcontextprotocol.spec.McpSchema;

/** A tool result that refuses the call with feedback the caller can act on. */
final class McpToolRefusal {
  private McpToolRefusal() {}

  static McpSchema.CallToolResult because(String message) {
    return McpSchema.CallToolResult.builder().isError(true).addTextContent(message).build();
  }
}

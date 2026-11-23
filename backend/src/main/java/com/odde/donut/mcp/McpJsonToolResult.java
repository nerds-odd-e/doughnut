package com.odde.donut.mcp;

import com.odde.donut.configs.ObjectMapperConfig;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.json.JsonMapper;

/** A tool result carrying one value as JSON text and as the same structured content. */
final class McpJsonToolResult {
  static final JsonMapper JSON = ObjectMapperConfig.handWrittenJsonMapper();

  private McpJsonToolResult() {}

  static McpSchema.CallToolResult of(Object value) {
    String serialized = JSON.writeValueAsString(value);
    return McpSchema.CallToolResult.builder()
        .addContent(new McpSchema.TextContent(serialized))
        .structuredContent(JSON.readValue(serialized, Object.class))
        .build();
  }
}

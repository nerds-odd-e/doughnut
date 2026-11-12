package com.odde.donut.mcp;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import java.io.IOException;
import java.io.UncheckedIOException;

/** A tool result carrying one value as JSON text and as the same structured content. */
final class McpJsonToolResult {
  private McpJsonToolResult() {}

  static McpSchema.CallToolResult of(JsonMapper objectMapper, Object value) {
    try {
      String serialized = objectMapper.writeValueAsString(value);
      return McpSchema.CallToolResult.builder()
          .addContent(new McpSchema.TextContent(serialized))
          .structuredContent(objectMapper.readValue(serialized, Object.class))
          .build();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}

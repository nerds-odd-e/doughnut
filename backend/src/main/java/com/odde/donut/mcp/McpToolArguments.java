package com.odde.donut.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Shared MCP tools/call argument binding for note-scoped tools. */
final class McpToolArguments {
  private McpToolArguments() {}

  static Integer noteId(McpSchema.CallToolRequest request) {
    Map<String, Object> arguments = request.arguments();
    if (arguments == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "noteId is required");
    }
    Object noteId = arguments.get("noteId");
    if (noteId instanceof Number number) {
      return number.intValue();
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "noteId is required");
  }

  /**
   * Preserve deliberate checked failures for {@link
   * com.odde.donut.factoryServices.FailureReportFactory#isDeliberate}.
   */
  @SuppressWarnings("unchecked")
  static <T extends Throwable> RuntimeException rethrow(Throwable failure) throws T {
    throw (T) failure;
  }
}

package com.odde.donut.mcp;

import com.odde.donut.entities.repositories.FailureReportRepository;
import com.odde.donut.factoryServices.FailureReportFactory;
import com.odde.donut.testability.TestabilitySettings;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.stereotype.Component;

/**
 * Maps deliberate MCP/tool failures to protocol errors and preserves {@link FailureReportFactory}
 * for unexpected failures even when the SDK turns them into protocol responses.
 */
@Component
public class McpFailureReporter {
  private final FailureReportRepository failureReportRepository;
  private final TestabilitySettings testabilitySettings;

  public McpFailureReporter(
      FailureReportRepository failureReportRepository, TestabilitySettings testabilitySettings) {
    this.failureReportRepository = failureReportRepository;
    this.testabilitySettings = testabilitySettings;
  }

  public McpSchema.JSONRPCResponse.JSONRPCError toJsonRpcError(Throwable failure) {
    if (failure instanceof McpError mcpError) {
      return mcpError.getJsonRpcError();
    }
    if (FailureReportFactory.isDeliberate(failure)) {
      return McpError.builder(-32000).message(failure.getMessage()).build().getJsonRpcError();
    }
    reportUnexpected(failure);
    return McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
        .message("Unexpected error: " + failure.getMessage())
        .build()
        .getJsonRpcError();
  }

  public void reportUnexpected(Throwable failure) {
    if (!(failure instanceof Exception exception) || failure instanceof McpError) {
      return;
    }
    FailureReportFactory.fromException(
            exception,
            "mcp",
            testabilitySettings.getGithubService(),
            failureReportRepository,
            testabilitySettings.getCurrentUTCTimestamp())
        .createUnlessAllowed();
  }
}

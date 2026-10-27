package com.odde.donut.mcp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

import com.odde.donut.entities.repositories.FailureReportRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.testability.SpringTestBase;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class McpFailureReporterTest extends SpringTestBase {
  @Autowired McpFailureReporter mcpFailureReporter;
  @Autowired FailureReportRepository failureReportRepository;

  @Test
  void deliberateAccessRefusalBecomesProtocolErrorWithoutFailureReport() {
    long before = failureReportRepository.count();

    var error = mcpFailureReporter.toJsonRpcError(new UnexpectedNoAccessRightException());

    assertThat(error.code(), equalTo(-32000));
    assertThat(failureReportRepository.count(), equalTo(before));
  }

  @Test
  void unexpectedFailureIsReportedAndMappedToInternalError() {
    long before = failureReportRepository.count();

    var error = mcpFailureReporter.toJsonRpcError(new IllegalStateException("boom"));

    assertThat(error.code(), equalTo(McpSchema.ErrorCodes.INTERNAL_ERROR));
    assertThat(failureReportRepository.count(), greaterThan(before));
  }

  @Test
  void responseStatusExceptionIsDeliberate() {
    long before = failureReportRepository.count();

    var error =
        mcpFailureReporter.toJsonRpcError(
            new ResponseStatusException(HttpStatus.BAD_REQUEST, "bad argument"));

    assertThat(error.message(), containsString("bad argument"));
    assertThat(failureReportRepository.count(), equalTo(before));
  }

  @Test
  void mcpErrorPassesThrough() {
    McpError original = McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS).message("x").build();

    assertThat(mcpFailureReporter.toJsonRpcError(original), equalTo(original.getJsonRpcError()));
  }
}

package com.odde.donut.mcp;

final class McpToolFailures {
  private McpToolFailures() {}

  /**
   * Preserve deliberate checked failures for {@link
   * com.odde.donut.factoryServices.FailureReportFactory#isDeliberate}.
   */
  @SuppressWarnings("unchecked")
  static <T extends Throwable> RuntimeException rethrow(Throwable failure) throws T {
    throw (T) failure;
  }
}

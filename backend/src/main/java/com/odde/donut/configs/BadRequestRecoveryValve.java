package com.odde.donut.configs;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.coyote.ActionCode;
import org.springframework.core.io.ClassPathResource;

public class BadRequestRecoveryValve extends ErrorReportValve {
  private final String recoveryPage;

  public BadRequestRecoveryValve() throws IOException {
    try (InputStream resource =
        new ClassPathResource("bad-request-recovery.html").getInputStream()) {
      recoveryPage = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Override
  protected void report(Request request, Response response, Throwable throwable) {
    if (response.getStatus() != 400) {
      super.report(request, response, throwable);
      return;
    }
    if (response.getContentWritten() > 0 || !response.setErrorReported()) return;

    AtomicBoolean ioAllowed = new AtomicBoolean(false);
    response.getCoyoteResponse().action(ActionCode.IS_IO_ALLOWED, ioAllowed);
    if (!ioAllowed.get()) return;

    response.setContentType("text/html");
    response.setCharacterEncoding(StandardCharsets.UTF_8);
    try {
      Writer writer = response.getReporter();
      if (writer != null) {
        writer.write(recoveryPage);
        response.finishResponse();
      }
    } catch (IOException exception) {
      getContainer().getLogger().warn("Could not send the request recovery page", exception);
    }
  }
}

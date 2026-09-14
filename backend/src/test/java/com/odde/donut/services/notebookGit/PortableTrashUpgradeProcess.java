package com.odde.donut.services.notebookGit;

import com.odde.donut.DonutApplication;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class PortableTrashUpgradeProcess {

  private static final Duration PROCESS_TIMEOUT = Duration.ofMinutes(2);
  private static final Duration TERMINATION_TIMEOUT = Duration.ofSeconds(10);

  private PortableTrashUpgradeProcess() {}

  static Result run(PreUpgradeFixtureSchema schema) throws Exception {
    String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
    ProcessBuilder processBuilder =
        new ProcessBuilder(
                List.of(
                    javaExecutable,
                    "-Dodd-e.donut.task=upgradePortableTrash",
                    "-Dspring.profiles.active=test",
                    "-cp",
                    System.getProperty("java.class.path"),
                    DonutApplication.class.getName()))
            .redirectErrorStream(true);
    processBuilder.environment().put("SPRING_DATASOURCE_URL", schema.jdbcUrl());
    processBuilder.environment().put("SPRING_DATASOURCE_USERNAME", schema.jdbcUsername());
    processBuilder.environment().put("SPRING_DATASOURCE_PASSWORD", schema.jdbcPassword());

    Process process = processBuilder.start();
    ExecutorService outputReader = Executors.newVirtualThreadPerTaskExecutor();
    ProcessOutput output = new ProcessOutput();
    Future<?> capturedOutput =
        outputReader.submit(
            () -> {
              output.readFrom(process.getInputStream());
              return null;
            });

    try {
      if (!process.waitFor(PROCESS_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
        process.destroyForcibly();
        if (!process.waitFor(TERMINATION_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
          throw new AssertionError(
              "Portable trash upgrade process did not terminate after timeout:\n"
                  + output.contents());
        }
        awaitOutputCapture(capturedOutput, output);
        throw new AssertionError(
            "Portable trash upgrade process timed out after "
                + PROCESS_TIMEOUT
                + ":\n"
                + output.contents());
      }

      awaitOutputCapture(capturedOutput, output);
      return new Result(process.exitValue(), output.contents());
    } finally {
      if (process.isAlive()) {
        process.destroyForcibly();
      }
      capturedOutput.cancel(true);
      outputReader.shutdownNow();
    }
  }

  private static void awaitOutputCapture(Future<?> capturedOutput, ProcessOutput output) {
    try {
      capturedOutput.get(TERMINATION_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while capturing portable trash upgrade output", e);
    } catch (ExecutionException e) {
      throw new AssertionError(
          "Failed to capture portable trash upgrade output:\n" + output.contents(), e.getCause());
    } catch (TimeoutException e) {
      throw new AssertionError(
          "Timed out while capturing portable trash upgrade output:\n" + output.contents(), e);
    }
  }

  private static final class ProcessOutput {
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    private void readFrom(InputStream input) throws IOException {
      byte[] buffer = new byte[8192];
      int byteCount;
      while ((byteCount = input.read(buffer)) != -1) {
        append(buffer, byteCount);
      }
    }

    private synchronized void append(byte[] buffer, int byteCount) {
      bytes.write(buffer, 0, byteCount);
    }

    private synchronized String contents() {
      return bytes.toString(StandardCharsets.UTF_8);
    }
  }

  record Result(int exitCode, String output) {}
}

package com.odde.doughnut.configs;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.IThrowableProxy;
import java.util.ArrayList;
import java.util.List;

public class TruncatedStackTraceConverter extends ThrowableProxyConverter {
  private static final int MAX_STACKTRACE_LINES = 20;

  @Override
  protected String throwableProxyToString(IThrowableProxy tp) {
    String fullStackTrace = super.throwableProxyToString(tp);
    if (fullStackTrace == null || fullStackTrace.isEmpty()) {
      return fullStackTrace;
    }
    return truncateAndFilterStacktrace(fullStackTrace);
  }

  private String truncateAndFilterStacktrace(String trace) {
    String[] lines = trace.split("\n");
    List<String> result = new ArrayList<>();
    int springInternalCount = 0;
    int totalLines = 0;

    for (String line : lines) {
      boolean isSpringInternal = isSpringInternal(line);

      if (isSpringInternal) {
        springInternalCount++;
      } else {
        if (springInternalCount > 0) {
          result.add("... " + springInternalCount + " lines of spring internal calls");
          springInternalCount = 0;
        }
        if (totalLines < MAX_STACKTRACE_LINES) {
          result.add(line);
          totalLines++;
        } else {
          break;
        }
      }
    }

    if (springInternalCount > 0) {
      result.add("... " + springInternalCount + " lines of spring internal calls");
    }

    if (lines.length > totalLines + springInternalCount) {
      int remaining = lines.length - totalLines - springInternalCount;
      result.add("... (" + remaining + " more lines)");
    }

    return String.join("\n", result);
  }

  private boolean isSpringInternal(String line) {
    if (line.trim().isEmpty()) {
      return false;
    }
    return line.contains("org.springframework.")
        || line.contains("org.hibernate.")
        || line.contains("java.lang.reflect.")
        || line.contains("sun.reflect.")
        || line.contains("jdk.internal.reflect.")
        || line.contains("org.apache.")
        || line.contains("jakarta.servlet.")
        || line.contains("org.eclipse.")
        || line.contains("com.sun.proxy.")
        || line.contains("org.aopalliance.");
  }
}

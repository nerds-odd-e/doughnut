package com.odde.donut.controllers;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.controllers.currentUser.CurrentUser;
import com.odde.donut.entities.User;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Shared bounded coordination and independent request state for concurrent controller calls. */
final class ConcurrentControllerTestSupport {
  private ConcurrentControllerTestSupport() {}

  /** Gives each worker a fresh request and its own current-user slot, then clears both. */
  static <T> Callable<T> inIsolatedRequest(
      CurrentUser currentUser, User ownerUser, Callable<T> task) {
    return () -> {
      RequestContextHolder.setRequestAttributes(
          new ServletRequestAttributes(new MockHttpServletRequest()));
      currentUser.setUser(ownerUser);
      try {
        return task.call();
      } finally {
        RequestContextHolder.resetRequestAttributes();
        currentUser.setUser(null);
      }
    };
  }

  static void assertQueued(Future<?> writer) {
    assertThrows(TimeoutException.class, () -> writer.get(250, TimeUnit.MILLISECONDS));
  }

  static void await(CountDownLatch latch) {
    try {
      if (!latch.await(10, TimeUnit.SECONDS)) {
        throw new AssertionError("Timed out waiting for test coordination");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(exception);
    }
  }
}

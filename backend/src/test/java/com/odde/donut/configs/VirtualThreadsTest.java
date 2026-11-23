package com.odde.donut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.odde.donut.testability.SpringTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;

class VirtualThreadsTest extends SpringTestBase {
  @Autowired
  @Qualifier("applicationTaskExecutor")
  AsyncTaskExecutor applicationTaskExecutor;

  @Test
  void applicationTasksRunOnVirtualThreads() throws Exception {
    assertThat(
        applicationTaskExecutor.submit(() -> Thread.currentThread().isVirtual()).get(), is(true));
  }
}

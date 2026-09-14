package com.odde.donut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.function.BiConsumer;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

class DonutTaskRunnerTest {

  @Test
  void upgradesPortableTrashReportsSuccessAndCloses() {
    ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
    Flyway flyway = mock(Flyway.class);
    when(context.getBean(Flyway.class)).thenReturn(flyway);

    CapturedTask captured =
        captureOutput(() -> new DonutTaskRunner(context).upgradePortableTrash());

    assertThat(captured.exitCode(), is(0));
    assertThat(captured.output(), containsString(DonutTaskRunner.PORTABLE_TRASH_UPGRADE_SUCCESS));
    var ordered = inOrder(context, flyway);
    ordered.verify(context).getBean(Flyway.class);
    ordered.verify(flyway).repair();
    ordered.verify(flyway).migrate();
    ordered.verify(context).close();
    verify(flyway, times(1)).repair();
    verify(flyway, times(1)).migrate();
  }

  @Test
  void closesAndFailsWithoutSuccessWhenFlywayCannotBeObtained() {
    assertPortableTrashUpgradeFailure(
        (context, flyway) ->
            when(context.getBean(Flyway.class))
                .thenThrow(new IllegalStateException("Flyway unavailable")));
  }

  @Test
  void closesAndFailsWithoutSuccessWhenFlywayRepairThrows() {
    assertPortableTrashUpgradeFailure(
        (context, flyway) ->
            doThrow(new IllegalStateException("repair failed")).when(flyway).repair());
  }

  @Test
  void closesAndFailsWithoutSuccessWhenFlywayMigrationThrows() {
    assertPortableTrashUpgradeFailure(
        (context, flyway) ->
            doThrow(new IllegalStateException("migration failed")).when(flyway).migrate());
  }

  private void assertPortableTrashUpgradeFailure(
      BiConsumer<ConfigurableApplicationContext, Flyway> provokeFailure) {
    ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
    Flyway flyway = mock(Flyway.class);
    when(context.getBean(Flyway.class)).thenReturn(flyway);
    provokeFailure.accept(context, flyway);

    CapturedTask captured =
        captureOutput(() -> new DonutTaskRunner(context).upgradePortableTrash());

    assertThat(captured.exitCode(), is(-1));
    assertThat(
        captured.output(), not(containsString(DonutTaskRunner.PORTABLE_TRASH_UPGRADE_SUCCESS)));
    verify(context, times(1)).close();
  }

  @Test
  void failsWithoutSuccessWhenClosingThePortableTrashUpgradeContextThrows() {
    ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
    Flyway flyway = mock(Flyway.class);
    when(context.getBean(Flyway.class)).thenReturn(flyway);
    doThrow(new IllegalStateException("close failed")).when(context).close();

    CapturedTask captured =
        captureOutput(() -> new DonutTaskRunner(context).upgradePortableTrash());

    assertThat(captured.exitCode(), is(-1));
    assertThat(
        captured.output(), not(containsString(DonutTaskRunner.PORTABLE_TRASH_UPGRADE_SUCCESS)));
    verify(context, times(1)).close();
  }

  private CapturedTask captureOutput(Task task) {
    PrintStream originalOut = System.out;
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(output));
      return new CapturedTask(task.run(), output.toString());
    } finally {
      System.setOut(originalOut);
    }
  }

  private interface Task {
    int run();
  }

  private record CapturedTask(int exitCode, String output) {}
}

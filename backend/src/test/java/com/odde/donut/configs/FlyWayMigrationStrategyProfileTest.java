package com.odde.donut.configs;

import static com.odde.donut.DonutApplication.PORTABLE_TRASH_UPGRADE_PROFILE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Collection;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class FlyWayMigrationStrategyProfileTest {

  @Test
  void productionProfileSelectsOnlyTheNoOpStartupStrategy() {
    Flyway flyway = mock(Flyway.class);

    migrateWithSelectedStrategy(flyway, "prod");

    verifyNoInteractions(flyway);
  }

  @Test
  void testProfileSelectsOnlyTheRepairAndMigrateStartupStrategy() {
    Flyway flyway = mock(Flyway.class);

    migrateWithSelectedStrategy(flyway, "test");

    verify(flyway).repair();
    verify(flyway).migrate();
  }

  @Test
  void portableTrashUpgradeSelectsOnlyTheNoOpStartupStrategy() {
    Flyway flyway = mock(Flyway.class);

    migrateWithSelectedStrategy(flyway, "test", PORTABLE_TRASH_UPGRADE_PROFILE);

    verifyNoInteractions(flyway);
  }

  private void migrateWithSelectedStrategy(Flyway flyway, String... profiles) {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.getEnvironment().setActiveProfiles(profiles);
      context.register(
          FlyWayFreeVersionIgnoreMigrationStrategyConfig.class,
          FlyWayTestMigrationStrategyConfig.class);
      context.refresh();

      Collection<FlywayMigrationStrategy> strategies =
          context.getBeansOfType(FlywayMigrationStrategy.class).values();
      assertThat(strategies, hasSize(1));
      strategies.iterator().next().migrate(flyway);
    }
  }
}

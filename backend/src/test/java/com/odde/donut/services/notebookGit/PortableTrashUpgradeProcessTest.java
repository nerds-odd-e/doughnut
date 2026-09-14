package com.odde.donut.services.notebookGit;

import static com.odde.donut.configs.DonutTaskRunner.PORTABLE_TRASH_UPGRADE_SUCCESS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PortableTrashUpgradeProcessTest {

  @Autowired DataSource dataSource;

  @Test
  void realTaskProcessUpgradesAnOwnedV325SchemaExactlyOnce() throws Exception {
    String ownerSchemaName;
    try (Connection connection = dataSource.getConnection()) {
      ownerSchemaName = connection.getCatalog();
    }

    try (PreUpgradeFixtureSchema fixture =
        PreUpgradeFixtureSchema.createAtVersion325(ownerSchemaName)) {
      PortableTrashUpgradeProcess.Result result = PortableTrashUpgradeProcess.run(fixture);

      assertThat(result.output(), result.exitCode(), equalTo(0));
      assertThat(
          result.output(),
          result.output().lines().filter(PORTABLE_TRASH_UPGRADE_SUCCESS::equals).toList(),
          contains(PORTABLE_TRASH_UPGRADE_SUCCESS));

      try (Statement statement = fixture.connection().createStatement();
          ResultSet history =
              statement.executeQuery(
                  "SELECT version, COUNT(*) AS successful_count"
                      + " FROM flyway_schema_history WHERE success = 1"
                      + " GROUP BY version ORDER BY MAX(installed_rank) DESC LIMIT 1")) {
        assertThat(history.next(), is(true));
        assertThat(history.getString("version"), equalTo("300000328"));
        assertThat(history.getInt("successful_count"), equalTo(1));
      }
    }
  }
}

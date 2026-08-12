package com.odde.doughnut.configs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrphanSoftDeletedMemoryTrackerCleanupMigrationTest {

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  @Test
  void hardDeletesSoftDeletedTrackersOnLiveNotes() throws Exception {
    Note liveNote = makeMe.aNote().please();
    Timestamp deletedAt = makeMe.aTimestamp().of(1, 1).please();
    MemoryTracker orphan = makeMe.aMemoryTrackerFor(liveNote).deletedAt(deletedAt).please();
    MemoryTracker active = makeMe.aMemoryTrackerFor(liveNote).spelling().please();

    runMigration();

    assertThat(trackerExists(orphan.getId()), is(false));
    assertThat(trackerExists(active.getId()), is(true));
  }

  @Test
  void keepsSoftDeletedTrackersOnSoftDeletedNotes() throws Exception {
    Note deletedNote = makeMe.aNote().please();
    Timestamp deletedAt = makeMe.aTimestamp().of(1, 1).please();
    MemoryTracker cascaded = makeMe.aMemoryTrackerFor(deletedNote).deletedAt(deletedAt).please();
    jdbcTemplate.update(
        "UPDATE note SET deleted_at = ? WHERE id = ?", deletedAt, deletedNote.getId());

    runMigration();

    assertThat(trackerExists(cascaded.getId()), is(true));
    assertThat(trackerDeletedAt(cascaded.getId()), is(not(nullValue())));
  }

  private void runMigration() throws Exception {
    makeMe.entityPersister.flush();
    String migrationSql =
        StreamUtils.copyToString(
            new ClassPathResource(
                    "db/migration/V300000245__hard_delete_orphan_soft_deleted_memory_trackers.sql")
                .getInputStream(),
            StandardCharsets.UTF_8);
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try (Statement statement = connection.createStatement()) {
      statement.executeUpdate(migrationSql);
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private boolean trackerExists(int id) {
    return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM memory_tracker WHERE id = ?", Integer.class, id)
        > 0;
  }

  private Timestamp trackerDeletedAt(int id) {
    return jdbcTemplate.queryForObject(
        "SELECT deleted_at FROM memory_tracker WHERE id = ?", Timestamp.class, id);
  }
}

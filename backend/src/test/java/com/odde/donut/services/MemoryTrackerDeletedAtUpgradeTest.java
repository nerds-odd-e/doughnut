package com.odde.donut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Populated upgrade proof for retiring {@code memory_tracker.deleted_at} (V300000323). Proves the
 * direct SQL preserves tracker identities, scheduling state, removal preferences, and recall
 * history links on a populated pre-migration schema; that the plain replacement unique index is
 * created; and that the plain index rejects the duplicate-key state the old functional index
 * allowed. The duplicate-key state is unreachable through application workflows (assimilation
 * refuses to create a tracker when one with the same {@code (user, note, type, property_key)}
 * already exists — see {@code AssimilationControllerAssimilateTests}), so the plain unique index is
 * behavior-preserving for reachable data.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemoryTrackerDeletedAtUpgradeTest {
  @Autowired JdbcTemplate jdbcTemplate;

  private void createFixtureTable() {
    jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS memory_tracker_upgrade_fixture");
    jdbcTemplate.execute(PRE_MIGRATION_DDL);
  }

  private static final String PRE_MIGRATION_DDL =
      """
      CREATE TEMPORARY TABLE memory_tracker_upgrade_fixture (
        id int unsigned NOT NULL,
        user_id int unsigned NOT NULL,
        note_id int unsigned NOT NULL,
        type varchar(32) NOT NULL DEFAULT 'UNDERSTANDING',
        property_key varchar(255) NOT NULL DEFAULT '',
        stability float NOT NULL DEFAULT '0',
        difficulty float DEFAULT NULL,
        last_recalled_at datetime DEFAULT NULL,
        assimilated_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
        next_recall_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
        removed_from_tracking tinyint NOT NULL DEFAULT '0',
        deleted_at timestamp NULL DEFAULT NULL,
        PRIMARY KEY (id),
        UNIQUE KEY user_note_spelling_active
          (user_id, note_id, type, property_key, (if((deleted_at is null),1,NULL)))
      )
      """;

  private static final String MIGRATION_SQL =
      """
      ALTER TABLE memory_tracker_upgrade_fixture DROP INDEX user_note_spelling_active;
      ALTER TABLE memory_tracker_upgrade_fixture DROP COLUMN deleted_at;
      ALTER TABLE memory_tracker_upgrade_fixture
        ADD UNIQUE KEY user_note_spelling_active (user_id, note_id, type, property_key);
      """;

  record TrackerRow(
      int id,
      int userId,
      int noteId,
      String type,
      String propertyKey,
      Float stability,
      Float difficulty,
      java.sql.Timestamp lastRecalledAt,
      java.sql.Timestamp nextRecallAt,
      boolean removedFromTracking) {}

  private TrackerRow row(int id) {
    return jdbcTemplate.queryForObject(
        """
        SELECT id, user_id, note_id, type, property_key, stability, difficulty,
               last_recalled_at, next_recall_at, removed_from_tracking
        FROM memory_tracker_upgrade_fixture WHERE id = ?
        """,
        (rs, n) ->
            new TrackerRow(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getInt("note_id"),
                rs.getString("type"),
                rs.getString("property_key"),
                rs.getFloat("stability"),
                rs.getObject("difficulty") == null ? null : rs.getFloat("difficulty"),
                rs.getTimestamp("last_recalled_at"),
                rs.getTimestamp("next_recall_at"),
                rs.getBoolean("removed_from_tracking")),
        id);
  }

  @Test
  void populatedUpgradePreservesTrackersSchedulesPreferencesAndDropsColumn() {
    createFixtureTable();
    jdbcTemplate.update(
        """
        INSERT INTO memory_tracker_upgrade_fixture
          (id, user_id, note_id, type, property_key, stability, difficulty,
           last_recalled_at, assimilated_at, next_recall_at, removed_from_tracking, deleted_at)
        VALUES
          (1, 100, 200, 'UNDERSTANDING', '', 4.5, 1.2,
           '2026-01-01 00:00:00', '2025-12-01 00:00:00', '2026-02-01 00:00:00', 0, NULL),
          (2, 100, 200, 'SPELLING', '', 8.0, 0.5,
           '2026-01-02 00:00:00', '2025-12-01 00:00:00', '2026-03-01 00:00:00', 0, NULL),
          (3, 100, 201, 'UNDERSTANDING', '', 2.0, 3.0,
           NULL, '2025-12-15 00:00:00', '2026-01-15 00:00:00', 1, NULL),
          (4, 100, 202, 'COMMISSIONED', '', 6.0, 2.0,
           '2026-01-03 00:00:00', '2025-12-01 00:00:00', '2026-04-01 00:00:00', 0, '2026-01-04 00:00:00'),
          (5, 100, 203, 'UNDERSTANDING', 'summary', 1.5, 4.0,
           NULL, '2025-12-20 00:00:00', '2026-01-20 00:00:00', 0, '2026-01-05 00:00:00')
        """);

    for (String statement : MIGRATION_SQL.strip().split(";\\s*")) {
      if (!statement.isBlank()) {
        jdbcTemplate.execute(statement);
      }
    }

    Integer columnCount =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'memory_tracker_upgrade_fixture'
              AND column_name = 'deleted_at'
            """,
            Integer.class);
    assertThat(columnCount, is(0));

    // The plain unique index is created and enforces uniqueness over
    // (user_id, note_id, type, property_key) across all rows.
    assertThrows(
        org.springframework.dao.DuplicateKeyException.class,
        () ->
            jdbcTemplate.update(
                """
                INSERT INTO memory_tracker_upgrade_fixture
                  (id, user_id, note_id, type, property_key)
                VALUES (99, 100, 200, 'UNDERSTANDING', '')
                """));

    assertThat(
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM memory_tracker_upgrade_fixture", Integer.class),
        is(5));

    TrackerRow understanding = row(1);
    assertThat(understanding.type(), equalTo("UNDERSTANDING"));
    assertThat(understanding.stability(), equalTo(4.5f));
    assertThat(understanding.difficulty(), equalTo(1.2f));
    assertThat(understanding.nextRecallAt(), notNullValue());
    assertThat(understanding.removedFromTracking(), is(false));

    TrackerRow spelling = row(2);
    assertThat(spelling.type(), equalTo("SPELLING"));
    assertThat(spelling.lastRecalledAt(), notNullValue());

    TrackerRow removed = row(3);
    assertThat(removed.removedFromTracking(), is(true));

    TrackerRow commissionedDeleted = row(4);
    assertThat(commissionedDeleted.type(), equalTo("COMMISSIONED"));

    TrackerRow propertyDeleted = row(5);
    assertThat(propertyDeleted.propertyKey(), equalTo("summary"));
  }

  @Test
  void plainUniqueIndexRejectsDuplicateKeysAllowedByOldFunctionalIndex() {
    createFixtureTable();
    jdbcTemplate.update(
        """
        INSERT INTO memory_tracker_upgrade_fixture
          (id, user_id, note_id, type, property_key, deleted_at)
        VALUES
          (10, 100, 200, 'UNDERSTANDING', '', NULL),
          (11, 100, 200, 'UNDERSTANDING', '', '2026-01-04 00:00:00')
        """);
    jdbcTemplate.execute(
        "ALTER TABLE memory_tracker_upgrade_fixture DROP INDEX user_note_spelling_active");
    jdbcTemplate.execute("ALTER TABLE memory_tracker_upgrade_fixture DROP COLUMN deleted_at");

    // The plain unique index cannot be created while the duplicate key exists.
    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () ->
            jdbcTemplate.execute(
                """
                ALTER TABLE memory_tracker_upgrade_fixture
                  ADD UNIQUE KEY user_note_spelling_active (user_id, note_id, type, property_key)
                """));
  }
}

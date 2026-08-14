package com.odde.doughnut.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Objects;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PropertyLevelDummySequenceSkipConversionTest {

  private static final String MIGRATION_SQL =
      "/db/migration/V300000255__convert_property_level_dummy_sequence_skips.sql";
  private static final String GATE_PLACEHOLDER = "${dummy_property_sequence_skip_convert}";
  private static final String PROPERTY_KEY = "topic";

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  MemoryTracker dummyTracker;
  MemoryTracker spellingTracker;
  MemoryTracker commissionedTracker;
  MemoryTracker noteLevelTracker;
  MemoryTracker recalledTracker;
  MemoryTracker alreadyDeletedTracker;
  MemoryTracker existingSkipTracker;
  MemoryTracker stillTracked;

  @BeforeEach
  void seedSelectionSiblings() {
    Note dummyNote = makeMe.aNote().please();
    dummyTracker =
        makeMe
            .aMemoryTrackerFor(dummyNote)
            .propertyKey(PROPERTY_KEY)
            .removedFromTracking()
            .please();
    spellingTracker =
        makeMe
            .aMemoryTrackerFor(dummyNote)
            .propertyKey(PROPERTY_KEY)
            .spelling()
            .removedFromTracking()
            .please();
    commissionedTracker =
        makeMe
            .aMemoryTrackerFor(dummyNote)
            .propertyKey(PROPERTY_KEY)
            .commissioned()
            .removedFromTracking()
            .please();
    noteLevelTracker = makeMe.aMemoryTrackerFor(dummyNote).removedFromTracking().please();

    recalledTracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().please())
            .propertyKey(PROPERTY_KEY)
            .removedFromTracking()
            .recallCount(1)
            .please();

    alreadyDeletedTracker =
        makeMe
            .aMemoryTrackerFor(makeMe.aNote().please())
            .propertyKey(PROPERTY_KEY)
            .removedFromTracking()
            .deletedAt(makeMe.aTimestamp().of(0, 0).please())
            .please();

    Note existingSkipNote = makeMe.aNote().please();
    existingSkipTracker =
        makeMe
            .aMemoryTrackerFor(existingSkipNote)
            .propertyKey(PROPERTY_KEY)
            .removedFromTracking()
            .please();
    makeMe.anAssimilationSequenceSkipFor(existingSkipNote).propertyKey(PROPERTY_KEY).please();

    stillTracked =
        makeMe.aMemoryTrackerFor(makeMe.aNote().please()).propertyKey(PROPERTY_KEY).please();
  }

  @Test
  void defaultGateDoesNotConvertDummyPropertySkip() {
    runConversion("1=0");

    assertThat(skipCount(dummyTracker, PROPERTY_KEY), is(0));
    assertThat(deletedAt(dummyTracker), nullValue());
  }

  @Nested
  class WhenGateIsEnabled {
    @BeforeEach
    void convert() {
      runConversion("1=1");
    }

    @Test
    void convertsPropertyLevelDummyUnderstandingSkip() {
      assertThat(skipCount(dummyTracker, PROPERTY_KEY), is(1));
      assertThat(deletedAt(dummyTracker), notNullValue());
    }

    @Test
    void doesNotConvertWhenRecallCountIsPositive() {
      assertThat(skipCount(recalledTracker, PROPERTY_KEY), is(0));
      assertThat(deletedAt(recalledTracker), nullValue());
    }

    @Test
    void doesNotConvertNoteLevelDummy() {
      assertThat(skipCount(noteLevelTracker, ""), is(0));
      assertThat(deletedAt(noteLevelTracker), nullValue());
    }

    @Test
    void doesNotConvertSpellingOrCommissionedDummies() {
      assertThat(deletedAt(spellingTracker), nullValue());
      assertThat(deletedAt(commissionedTracker), nullValue());
    }

    @Test
    void existingSkipDoesNotFailUniquenessAndStillSoftDeletes() {
      assertThat(skipCount(existingSkipTracker, PROPERTY_KEY), is(1));
      assertThat(deletedAt(existingSkipTracker), notNullValue());
    }

    @Test
    void doesNotConvertAlreadyDeletedDummy() {
      assertThat(skipCount(alreadyDeletedTracker, PROPERTY_KEY), is(0));
    }

    @Test
    void doesNotConvertLiveUnderstandingTracker() {
      assertThat(deletedAt(stillTracked), nullValue());
    }
  }

  private void runConversion(String gate) {
    makeMe.entityPersister.flush();
    String sql = loadMigrationSql().replace(GATE_PLACEHOLDER, gate);
    Connection connection = DataSourceUtils.getConnection(dataSource);
    try {
      ScriptUtils.executeSqlScript(
          connection, new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
    } finally {
      DataSourceUtils.releaseConnection(connection, dataSource);
    }
  }

  private String loadMigrationSql() {
    try (var in =
        Objects.requireNonNull(
            getClass().getResourceAsStream(MIGRATION_SQL), () -> "missing " + MIGRATION_SQL)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Could not read " + MIGRATION_SQL, e);
    }
  }

  private Integer skipCount(MemoryTracker tracker, String propertyKey) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM assimilation_sequence_skip"
            + " WHERE user_id = ? AND note_id = ? AND property_key = ?",
        Integer.class,
        tracker.getUser().getId(),
        tracker.getNote().getId(),
        propertyKey);
  }

  private Timestamp deletedAt(MemoryTracker tracker) {
    return jdbcTemplate.queryForObject(
        "SELECT deleted_at FROM memory_tracker WHERE id = ?", Timestamp.class, tracker.getId());
  }
}

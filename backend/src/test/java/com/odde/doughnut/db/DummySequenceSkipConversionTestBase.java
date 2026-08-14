package com.odde.doughnut.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.builders.MemoryTrackerBuilder;
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
abstract class DummySequenceSkipConversionTestBase {

  @Autowired MakeMe makeMe;
  @Autowired DataSource dataSource;
  @Autowired JdbcTemplate jdbcTemplate;

  MemoryTracker dummyTracker;
  MemoryTracker spellingTracker;
  MemoryTracker commissionedTracker;
  MemoryTracker recalledTracker;
  MemoryTracker alreadyDeletedTracker;
  MemoryTracker existingSkipTracker;
  MemoryTracker stillTracked;

  abstract String migrationSql();

  abstract String gatePlaceholder();

  abstract String grainPropertyKey();

  @BeforeEach
  void seedConvertedGrainSiblings() {
    Note dummyNote = makeMe.aNote().please();
    dummyTracker = dummyGrainTrackerFor(dummyNote).please();
    spellingTracker = dummyGrainTrackerFor(dummyNote).spelling().please();
    commissionedTracker = dummyGrainTrackerFor(dummyNote).commissioned().please();

    recalledTracker = dummyGrainTrackerFor(makeMe.aNote().please()).recallCount(1).please();

    alreadyDeletedTracker =
        dummyGrainTrackerFor(makeMe.aNote().please())
            .deletedAt(makeMe.aTimestamp().of(0, 0).please())
            .please();

    Note existingSkipNote = makeMe.aNote().please();
    existingSkipTracker = dummyGrainTrackerFor(existingSkipNote).please();
    makeMe.anAssimilationSequenceSkipFor(existingSkipNote).propertyKey(grainPropertyKey()).please();

    stillTracked = grainTrackerFor(makeMe.aNote().please()).please();
  }

  @Test
  void defaultGateDoesNotConvertDummySkip() {
    runConversion("1=0");

    assertThat(skipCount(dummyTracker), is(0));
    assertThat(deletedAt(dummyTracker), nullValue());
  }

  @Nested
  class WhenGateIsEnabled {
    @BeforeEach
    void convert() {
      runConversion("1=1");
    }

    @Test
    void convertsDummyUnderstandingSkip() {
      assertThat(skipCount(dummyTracker), is(1));
      assertThat(deletedAt(dummyTracker), notNullValue());
    }

    @Test
    void doesNotConvertWhenRecallCountIsPositive() {
      assertThat(skipCount(recalledTracker), is(0));
      assertThat(deletedAt(recalledTracker), nullValue());
    }

    @Test
    void doesNotConvertSpellingOrCommissionedDummies() {
      assertThat(deletedAt(spellingTracker), nullValue());
      assertThat(deletedAt(commissionedTracker), nullValue());
    }

    @Test
    void existingSkipDoesNotFailUniquenessAndStillSoftDeletes() {
      assertThat(skipCount(existingSkipTracker), is(1));
      assertThat(deletedAt(existingSkipTracker), notNullValue());
    }

    @Test
    void doesNotConvertAlreadyDeletedDummy() {
      assertThat(skipCount(alreadyDeletedTracker), is(0));
    }

    @Test
    void doesNotConvertLiveUnderstandingTracker() {
      assertThat(deletedAt(stillTracked), nullValue());
    }
  }

  Integer skipCount(MemoryTracker tracker) {
    return skipCount(tracker, grainPropertyKey());
  }

  Integer skipCount(MemoryTracker tracker, String propertyKey) {
    return jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM assimilation_sequence_skip"
            + " WHERE user_id = ? AND note_id = ? AND property_key = ?",
        Integer.class,
        tracker.getUser().getId(),
        tracker.getNote().getId(),
        propertyKey);
  }

  Timestamp deletedAt(MemoryTracker tracker) {
    return jdbcTemplate.queryForObject(
        "SELECT deleted_at FROM memory_tracker WHERE id = ?", Timestamp.class, tracker.getId());
  }

  private MemoryTrackerBuilder grainTrackerFor(Note note) {
    return makeMe.aMemoryTrackerFor(note).propertyKey(grainPropertyKey());
  }

  private MemoryTrackerBuilder dummyGrainTrackerFor(Note note) {
    return grainTrackerFor(note).removedFromTracking();
  }

  private void runConversion(String gate) {
    makeMe.entityPersister.flush();
    String sql = loadMigrationSql().replace(gatePlaceholder(), gate);
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
            getClass().getResourceAsStream(migrationSql()), () -> "missing " + migrationSql())) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Could not read " + migrationSql(), e);
    }
  }
}

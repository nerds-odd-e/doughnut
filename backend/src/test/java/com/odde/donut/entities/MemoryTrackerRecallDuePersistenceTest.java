package com.odde.donut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;

import com.odde.donut.testability.MakeMe;
import com.odde.donut.utils.TimestampOperations;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemoryTrackerRecallDuePersistenceTest {

  @Autowired MakeMe makeMe;
  @Autowired JdbcTemplate jdbcTemplate;

  @ParameterizedTest
  @CsvSource({"last_recalled_at, YES", "next_recall_at, NO"})
  void recallAtColumnIsDatetime(String columnName, String nullable) {
    String[] column =
        jdbcTemplate.queryForObject(
            """
            SELECT DATA_TYPE, IS_NULLABLE FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'memory_tracker' AND column_name = ?
            """,
            (rs, rowNum) -> new String[] {rs.getString("DATA_TYPE"), rs.getString("IS_NULLABLE")},
            columnName);
    assertThat(column[0], equalToIgnoringCase("datetime"));
    assertThat(column[1], equalToIgnoringCase(nullable));
  }

  @Test
  void persistsNextRecallAtLastPlusMaximumIntervalHours() {
    Timestamp last = makeMe.aTimestamp().of(0, 0).please();
    Timestamp due =
        TimestampOperations.addHoursToTimestamp(last, Math.round(Fsrs.MAXIMUM_INTERVAL_HOURS));
    MemoryTracker tracker =
        makeMe.aMemoryTrackerFor(makeMe.aNote().please()).nextRecallAt(due).please();
    makeMe.entityPersister.flush();

    Timestamp persisted =
        jdbcTemplate.queryForObject(
            "SELECT next_recall_at FROM memory_tracker WHERE id = ?",
            Timestamp.class,
            tracker.getId());
    assertThat(persisted, equalTo(due));
  }
}

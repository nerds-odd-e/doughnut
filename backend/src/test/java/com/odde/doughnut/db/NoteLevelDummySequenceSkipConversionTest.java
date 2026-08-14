package com.odde.doughnut.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.MemoryTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NoteLevelDummySequenceSkipConversionTest extends DummySequenceSkipConversionTestBase {

  MemoryTracker propertyTracker;

  @Override
  String migrationSql() {
    return "/db/migration/V300000254__convert_note_level_dummy_sequence_skips.sql";
  }

  @Override
  String gatePlaceholder() {
    return "${dummy_note_sequence_skip_convert}";
  }

  @Override
  String grainPropertyKey() {
    return "";
  }

  @BeforeEach
  void seedPropertyLevelSibling() {
    propertyTracker =
        makeMe
            .aMemoryTrackerFor(dummyTracker.getNote())
            .propertyKey("topic")
            .removedFromTracking()
            .please();
  }

  @Nested
  class WhenGateIsEnabled extends DummySequenceSkipConversionTestBase.WhenGateIsEnabled {
    @Test
    void doesNotConvertPropertyLevelDummy() {
      assertThat(skipCount(propertyTracker, "topic"), is(0));
      assertThat(deletedAt(propertyTracker), nullValue());
    }
  }
}

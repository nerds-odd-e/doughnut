package com.odde.doughnut.db;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.odde.doughnut.entities.MemoryTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PropertyLevelDummySequenceSkipConversionTest extends DummySequenceSkipConversionTestBase {

  MemoryTracker noteLevelTracker;

  @Override
  String migrationSql() {
    return "/db/migration/V300000255__convert_property_level_dummy_sequence_skips.sql";
  }

  @Override
  String gatePlaceholder() {
    return "${dummy_property_sequence_skip_convert}";
  }

  @Override
  String grainPropertyKey() {
    return "topic";
  }

  @BeforeEach
  void seedNoteLevelSibling() {
    noteLevelTracker =
        makeMe.aMemoryTrackerFor(dummyTracker.getNote()).removedFromTracking().please();
  }

  @Nested
  class WhenGateIsEnabled extends DummySequenceSkipConversionTestBase.WhenGateIsEnabled {
    @Test
    void doesNotConvertNoteLevelDummy() {
      assertThat(skipCount(noteLevelTracker, ""), is(0));
      assertThat(deletedAt(noteLevelTracker), nullValue());
    }
  }
}

package com.odde.donut.entities;

/**
 * JPQL WHERE fragments for tracker grain and type. Must stay aligned with {@link
 * MemoryTracker#isNoteLevelTracker()} and {@link MemoryTrackerType#UNDERSTANDING}.
 */
public final class MemoryTrackerQueryFragments {
  private MemoryTrackerQueryFragments() {}

  /** JPQL fragment for joined alias {@code rp}. */
  public static final String JPA_WHERE_NOTE_LEVEL_TRACKER =
      "(rp.propertyKey IS NULL OR rp.propertyKey = '')";

  /** JPQL fragment for joined alias {@code rp}. */
  public static final String JPA_WHERE_UNDERSTANDING_TRACKER =
      "rp.type = com.odde.donut.entities.MemoryTrackerType.UNDERSTANDING";
}

package com.odde.doughnut.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class WikiSlugGenerationTest {

  @Test
  void englishTitleBecomesLowercaseHyphenatedSlug() {
    assertEquals("hello-world", WikiSlugGeneration.toBaseSlug("Hello World"));
  }

  @Test
  void hiraganaTitleFallsBackWhenSlugifyProducesEmpty() {
    assertEquals("untitled", WikiSlugGeneration.toBaseSlug("どうやら"));
  }

  @Test
  void emptyInputUsesFallback() {
    assertEquals("untitled", WikiSlugGeneration.toBaseSlug(""));
  }

  @Test
  void punctuationOnlyUsesFallback() {
    assertEquals("untitled", WikiSlugGeneration.toBaseSlug("!!!"));
  }

  @Test
  void uniqueSlugSkipsTakenBaseWithNumericSuffix() {
    assertEquals(
        "hello-world-2", WikiSlugGeneration.uniqueSlugWithin("Hello World", Set.of("hello-world")));
  }

  @Test
  void uniqueSlugIncrementsUntilFree() {
    assertEquals(
        "hello-world-3",
        WikiSlugGeneration.uniqueSlugWithin("Hello World", Set.of("hello-world", "hello-world-2")));
  }

  @Test
  void uniqueSlugResolvesCollisionsForFallbackBase() {
    assertEquals("untitled-2", WikiSlugGeneration.uniqueSlugWithin("どうやら", Set.of("untitled")));
  }

  @Test
  void uniqueSlugWithinMaxLen_truncatesBaseWhenNeeded() {
    assertEquals("abc", WikiSlugGeneration.uniqueSlugWithinMaxLen("Abc Def Ghijk", Set.of(), 3));
  }

  @Test
  void uniqueSlugWithinMaxLen_numericSuffixWhenBaseTakenWithinBudget() {
    assertEquals(
        "hel-2", WikiSlugGeneration.uniqueSlugWithinMaxLen("Hello World", Set.of("hello"), 5));
  }
}

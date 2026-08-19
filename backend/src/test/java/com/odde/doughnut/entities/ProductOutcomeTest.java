package com.odde.doughnut.entities;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

class ProductOutcomeTest {

  @Test
  void mappedGradeSqlInListIsTheFourLiveGrades() {
    assertThat(ProductOutcome.mappedGradeSqlInList(), is("'GOOD', 'EASY', 'HARD', 'AGAIN'"));
  }
}

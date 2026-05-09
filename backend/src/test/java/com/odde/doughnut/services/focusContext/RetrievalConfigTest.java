package com.odde.doughnut.services.focusContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

class RetrievalConfigTest {

  @Test
  void forQuestionGeneration_default_uses_combined_content_budget() {
    RetrievalConfig config = RetrievalConfig.forQuestionGeneration(null);
    assertThat(
        config.getFocusContextContentTokenBudget(),
        equalTo(FocusContextConstants.FOCUS_CONTEXT_COMBINED_CONTENT_TOKEN_BUDGET));
  }

  @Test
  void forQuestionGeneration_with_budget_overrides_default() {
    int reducedBudget = FocusContextConstants.FOCUS_CONTEXT_COMBINED_CONTENT_TOKEN_BUDGET - 42;
    RetrievalConfig config = RetrievalConfig.forQuestionGeneration(null, reducedBudget);
    assertThat(config.getFocusContextContentTokenBudget(), equalTo(reducedBudget));
  }

  @Test
  void forQuestionGeneration_with_budget_preserves_seed_and_depth() {
    RetrievalConfig config = RetrievalConfig.forQuestionGeneration(99L, 1800);
    assertThat(config.getSampleSeed().orElse(null), equalTo(99L));
    assertThat(config.getMaxDepth(), equalTo(2));
    assertThat(config.getFocusContextContentTokenBudget(), equalTo(1800));
  }
}

package com.odde.donut.services.focusContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.jupiter.api.Test;

class RetrievalConfigTest {

  @Test
  void forQuestionGenerationDefaultUsesCombinedContentBudget() {
    RetrievalConfig config = RetrievalConfig.forQuestionGeneration(null);
    assertThat(
        config.getFocusContextContentTokenBudget(),
        equalTo(FocusContextConstants.FOCUS_CONTEXT_COMBINED_CONTENT_TOKEN_BUDGET));
  }

  @Test
  void forQuestionGenerationWithBudgetOverridesDefault() {
    int reducedBudget = FocusContextConstants.FOCUS_CONTEXT_COMBINED_CONTENT_TOKEN_BUDGET - 42;
    RetrievalConfig config = RetrievalConfig.forQuestionGeneration(null, reducedBudget);
    assertThat(config.getFocusContextContentTokenBudget(), equalTo(reducedBudget));
  }

  @Test
  void forQuestionGenerationWithBudgetPreservesSeedAndDepth() {
    RetrievalConfig config = RetrievalConfig.forQuestionGeneration(99L, 1800);
    assertThat(config.getSampleSeed().orElse(null), equalTo(99L));
    assertThat(config.getMaxDepth(), equalTo(2));
    assertThat(config.getFocusContextContentTokenBudget(), equalTo(1800));
  }
}

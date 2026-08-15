package com.odde.doughnut.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.odde.doughnut.services.ai.GeneratedMcq;
import com.odde.doughnut.services.ai.TitleReplacement;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import org.junit.jupiter.api.Test;

class AiOpenAiAssistantFactoryTest {

  @Test
  void assistantToolsIncludeTitleReplacementWithJsonDescription() {
    assertThat(AiToolFactory.getAllAssistantTools(), hasItem(TitleReplacement.class));
    JsonClassDescription annotation =
        TitleReplacement.class.getAnnotation(JsonClassDescription.class);
    assertThat(annotation, notNullValue());
    assertThat(annotation.value(), containsString("Generate a concise and accurate note title"));
  }

  @Test
  void assistantToolsIncludeQuestionGeneration() {
    assertThat(AiToolFactory.getAllAssistantTools(), hasItem(GeneratedMcq.class));
  }
}

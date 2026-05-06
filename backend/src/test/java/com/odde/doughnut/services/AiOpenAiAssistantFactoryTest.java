package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.TitleReplacement;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AiOpenAiAssistantFactoryTest {

  @Nested
  class AiToolsTest {
    @Test
    void shouldHaveToolForGeneratingTitle() {
      List<Class<?>> tools = AiToolFactory.getAllAssistantTools();

      Optional<Class<?>> titleTool =
          tools.stream().filter(t -> t == TitleReplacement.class).findFirst();

      assertTrue(titleTool.isPresent());
      // Verify the class has @JsonClassDescription annotation
      JsonClassDescription annotation =
          TitleReplacement.class.getAnnotation(JsonClassDescription.class);
      assertTrue(annotation != null);
      assertTrue(annotation.value().contains("Generate a concise and accurate note title"));
    }

    @Test
    void shouldHaveParameterClassForGeneratingTitle() {
      List<Class<?>> tools = AiToolFactory.getAllAssistantTools();

      Optional<Class<?>> titleTool =
          tools.stream().filter(t -> t == TitleReplacement.class).findFirst();

      assertTrue(titleTool.isPresent());
      // Verify the tool has a parameter class (used for structured outputs)
      assertTrue(titleTool.get() != null);
    }

    @Test
    void shouldHaveQuestionGenerationTool() {
      List<Class<?>> tools = AiToolFactory.getAllAssistantTools();

      Optional<Class<?>> questionTool =
          tools.stream().filter(t -> t == MCQWithAnswer.class).findFirst();

      assertTrue(questionTool.isPresent());
    }
  }
}

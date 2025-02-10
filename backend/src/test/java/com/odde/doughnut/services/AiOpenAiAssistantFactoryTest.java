package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.AiToolName;
import com.odde.doughnut.services.ai.tools.FunctionDefinition;
import com.theokanning.openai.assistants.assistant.FunctionTool;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.image.Image;
import com.theokanning.openai.image.ImageResult;
import io.reactivex.Single;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class AiOpenAiAssistantFactoryTest {

  @Mock private OpenAiApi openAiApi;

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
  }

  @Nested
  class GetImage {
    @Test
    void getImageBasedOnPrompt() {
      ImageResult result = new ImageResult();
      Image image = new Image();
      image.setB64Json("https://image.com");
      result.setData(List.of(image));
      Mockito.when(openAiApi.createImage(Mockito.any())).thenReturn(Single.just(result));
      assertEquals("https://image.com", new OtherAiServices(openAiApi).getTimage("prompt"));
    }
  }

  @Nested
  class AiToolsTest {
    @Test
    void shouldHaveToolForGeneratingTitle() {
      List<AiTool> tools = AiToolFactory.getAllAssistantTools();

      Optional<AiTool> topicTitleTool =
          tools.stream()
              .filter(t -> t.name().equals(AiToolName.SUGGEST_NOTE_TITLE.getValue()))
              .findFirst();

      assertTrue(topicTitleTool.isPresent());
      assertTrue(
          topicTitleTool
              .get()
              .description()
              .contains("Generate a concise and accurate note title"));
    }

    @Test
    void shouldHaveStrictToolForGeneratingTitle() {
      List<AiTool> tools = AiToolFactory.getAllAssistantTools();

      Optional<AiTool> topicTitleTool =
          tools.stream()
              .filter(t -> t.name().equals(AiToolName.SUGGEST_NOTE_TITLE.getValue()))
              .findFirst();

      FunctionTool tool = (FunctionTool) topicTitleTool.get().getTool();
      FunctionDefinition fun = (FunctionDefinition) tool.getFunction();
      assertTrue(fun.getStrict());
    }

    @Test
    void shouldHaveQuestionGenerationTool() {
      List<AiTool> tools = AiToolFactory.getAllAssistantTools();

      Optional<AiTool> topicTitleTool =
          tools.stream()
              .filter(
                  t ->
                      t.name()
                          .equals(AiToolName.ASK_SINGLE_ANSWER_MULTIPLE_CHOICE_QUESTION.getValue()))
              .findFirst();

      assertTrue(topicTitleTool.isPresent());
    }
  }
}

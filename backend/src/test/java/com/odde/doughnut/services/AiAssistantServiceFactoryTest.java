package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.AiToolName;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
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

class AiAssistantServiceFactoryTest {

  @Mock private OpenAiApi openAiApi;
  OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
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
    void shouldHaveToolForGeneratingTopicTitle() {
      List<AiTool> tools = AiToolFactory.getAllAssistantTools();

      Optional<AiTool> topicTitleTool =
          tools.stream()
              .filter(t -> t.name().equals(AiToolName.SUGGEST_NOTE_TOPIC_TITLE.getValue()))
              .findFirst();

      assertTrue(topicTitleTool.isPresent());
      assertTrue(
          topicTitleTool
              .get()
              .description()
              .contains("Generate a concise and accurate note topic"));
    }
  }
}

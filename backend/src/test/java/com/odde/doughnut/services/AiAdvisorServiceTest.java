package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
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

class AiAdvisorServiceTest {

  private AiAdvisorService aiAdvisorService;
  @Mock private OpenAiApi openAiApi;
  OpenAIChatCompletionMock openAIChatCompletionMock;

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
    aiAdvisorService = new AiAdvisorService(openAiApi);
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
      assertEquals("https://image.com", aiAdvisorService.getOtherAiServices().getTimage("prompt"));
    }
  }

  @Nested
  class AiToolsTest {
    @Test
    void shouldHaveToolForGeneratingTopicTitle() {
      List<AiTool> tools = AiToolFactory.getCompletionAiTools();

      Optional<AiTool> topicTitleTool =
          tools.stream()
              .filter(t -> t.name().equals(AiToolFactory.GENERATE_TOPIC_TITLE))
              .findFirst();

      assertTrue(topicTitleTool.isPresent());
      assertEquals(
          "Generate a concise and descriptive title based on the note content",
          topicTitleTool.get().description());
    }
  }
}

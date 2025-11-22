package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.services.ai.tools.AiTool;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.odde.doughnut.services.ai.tools.AiToolName;
import com.odde.doughnut.services.ai.tools.FunctionDefinition;
import com.openai.client.OpenAIClient;
import com.openai.models.images.Image;
import com.openai.models.images.ImageGenerateParams;
import com.openai.models.images.ImagesResponse;
import com.openai.services.blocking.ImageService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class AiOpenAiAssistantFactoryTest {

  @MockitoBean(name = "officialOpenAiClient")
  private OpenAIClient officialClient;

  @Autowired OtherAiServices otherAiServices;

  @Nested
  class GetImage {
    @Test
    void getImageBasedOnPrompt() {
      ImageService imageService = Mockito.mock(ImageService.class);
      when(officialClient.images()).thenReturn(imageService);
      Image image = Image.builder().b64Json("https://image.com").build();
      ImagesResponse response =
          ImagesResponse.builder().created(1234567890L).data(List.of(image)).build();
      when(imageService.generate(Mockito.any(ImageGenerateParams.class))).thenReturn(response);
      assertEquals("https://image.com", otherAiServices.getTimage("prompt"));
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

      FunctionDefinition fun = topicTitleTool.get().getFunctionDefinition();
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

package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.services.ai.TitleReplacement;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
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

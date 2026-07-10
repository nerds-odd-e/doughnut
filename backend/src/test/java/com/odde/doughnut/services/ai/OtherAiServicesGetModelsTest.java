package com.odde.doughnut.services.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.odde.doughnut.services.openAiApis.OpenAiApiHandler;
import com.openai.models.models.Model;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtherAiServicesGetModelsTest {

  private OpenAiApiHandler openAiApiHandler;
  private OtherAiServices otherAiServices;

  @BeforeEach
  void setup() {
    openAiApiHandler = mock(OpenAiApiHandler.class);
    otherAiServices = new OtherAiServices(openAiApiHandler);
  }

  @Test
  void returnsGptAndFineTunedModelIds() {
    Model gptModel = Model.builder().id("gpt-4").created(1L).ownedBy("openai").build();
    Model otherModel = Model.builder().id("dall-e-3").created(1L).ownedBy("openai").build();
    when(openAiApiHandler.getModels()).thenReturn(List.of(gptModel, otherModel));

    assertThat(otherAiServices.getAvailableGptModels()).containsExactly("gpt-4");
  }
}

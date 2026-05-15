package com.odde.doughnut.services.openAiApis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.NoteContentCompletion;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolFactory;
import com.openai.client.OpenAIClient;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OpenAiApiHandlerJsonSchemaToolsGuardTest {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired OpenAiApiHandler openAiApiHandler;

  @Test
  void shouldThrowWhenJsonSchemaPathUsedWithTools() {
    ChatCompletionCreateParams requestWithTools =
        ChatCompletionCreateParams.builder()
            .model(ChatModel.of(GlobalSettingsService.DEFAULT_CHAT_MODEL))
            .messages(
                List.of(
                    ChatCompletionMessageParam.ofUser(
                        ChatCompletionUserMessageParam.builder().content("test").build())))
            .addTool(NoteContentCompletion.class)
            .build();

    OpenAIChatRequestBuilder builderWithTools =
        new OpenAIChatRequestBuilder() {
          @Override
          public ChatCompletionCreateParams build() {
            return requestWithTools;
          }
        };

    RuntimeException exception =
        assertThrows(
            RuntimeException.class,
            () ->
                openAiApiHandler.requestAndGetJsonSchemaResult(
                    AiToolFactory.suggestNoteTitleAiTool(), builderWithTools));

    assertThat(
        exception.getMessage(),
        containsString("requestAndGetJsonSchemaResult must not be used with tools"));
  }
}

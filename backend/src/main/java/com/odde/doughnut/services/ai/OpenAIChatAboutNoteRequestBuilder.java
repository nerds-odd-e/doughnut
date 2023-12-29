package com.odde.doughnut.services.ai;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.builder.OpenAIChatRequestBuilder;
import com.odde.doughnut.services.ai.tools.AiToolList;
import com.theokanning.openai.completion.chat.*;
import java.util.List;

public class OpenAIChatAboutNoteRequestBuilder {
  protected final OpenAIChatRequestBuilder openAIChatRequestBuilder;

  public OpenAIChatAboutNoteRequestBuilder(String modelName, Note note) {
    openAIChatRequestBuilder =
        OpenAIChatRequestBuilder.chatAboutNoteRequestBuilder(modelName, note);
  }

  public OpenAIChatAboutNoteRequestBuilder addTool(AiToolList tool) {
    openAIChatRequestBuilder.addTool(tool);
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder addMessages(List<ChatMessage> messages) {
    openAIChatRequestBuilder.messages.addAll(messages);

    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder chatMessage(String userMessage) {
    openAIChatRequestBuilder.addUserMessage(userMessage);
    return this;
  }

  public OpenAIChatAboutNoteRequestBuilder maxTokens(int maxTokens) {
    openAIChatRequestBuilder.maxTokens(maxTokens);
    return this;
  }

  public ChatCompletionRequest build() {
    return openAIChatRequestBuilder.build();
  }
}

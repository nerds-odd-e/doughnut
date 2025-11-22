package com.odde.doughnut.services.ai.builder;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.openai.models.ChatModel;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import java.util.ArrayList;
import java.util.List;

public class OpenAIChatRequestBuilder {
  public static final String systemInstruction =
      "This is a PKM system using hierarchical notes, each with a title and details, to capture atomic concepts.";
  public final List<ChatCompletionMessageParam> messages = new ArrayList<>();
  ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder();

  public static OpenAIChatRequestBuilder chatAboutNoteRequestBuilder(String modelName, Note note) {
    return new OpenAIChatRequestBuilder()
        .model(modelName)
        .addSystemMessage(systemInstruction)
        .addSystemMessage(note.getNoteDescription());
  }

  public OpenAIChatRequestBuilder model(String modelName) {
    builder.model(ChatModel.of(modelName));
    return this;
  }

  public OpenAIChatRequestBuilder maxTokens(int maxTokens) {
    builder.maxTokens(maxTokens);
    return this;
  }

  public OpenAIChatRequestBuilder responseJsonSchema(InstructionAndSchema tool) {
    addUserMessage(tool.getMessageBody());
    // Use official SDK's responseFormat with schema class
    if (tool.getParameterClass() != null) {
      @SuppressWarnings("unchecked")
      Class<Object> schemaClass = (Class<Object>) tool.getParameterClass();
      builder.responseFormat(schemaClass);
    }
    return this;
  }

  public List<ChatCompletionMessageParam> buildMessages() {
    return messages;
  }

  public ChatCompletionCreateParams build() {
    ChatCompletionCreateParams.Builder requestBuilder = builder.messages(messages).n(1L);
    return requestBuilder.build();
  }

  public OpenAIChatRequestBuilder addSystemMessage(String message) {
    messages.add(
        ChatCompletionMessageParam.ofSystem(
            ChatCompletionSystemMessageParam.builder().content(message).build()));
    return this;
  }

  public OpenAIChatRequestBuilder addUserMessage(String message) {
    messages.add(
        ChatCompletionMessageParam.ofUser(
            ChatCompletionUserMessageParam.builder().content(message).build()));
    return this;
  }
}

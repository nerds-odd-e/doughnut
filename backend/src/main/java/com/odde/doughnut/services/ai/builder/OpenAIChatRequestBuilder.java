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
import java.util.stream.Collectors;

public class OpenAIChatRequestBuilder {
  public static final String systemInstruction =
      "This is a PKM system using hierarchical notes, each with a title and details, to capture atomic concepts.";
  public final List<ChatCompletionMessageParam> messages = new ArrayList<>();
  private final List<String> overallSystemMessages = new ArrayList<>();
  ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder();

  public static OpenAIChatRequestBuilder chatAboutNoteRequestBuilder(String modelName, Note note) {
    return new OpenAIChatRequestBuilder()
        .model(modelName)
        .addToOverallSystemMessage(systemInstruction)
        .addToOverallSystemMessage(note.getNoteDescription());
  }

  public OpenAIChatRequestBuilder model(String modelName) {
    builder.model(ChatModel.of(modelName));
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
    List<ChatCompletionMessageParam> finalMessages = new ArrayList<>();
    if (!overallSystemMessages.isEmpty()) {
      String joinedSystemMessage =
          overallSystemMessages.stream().collect(Collectors.joining("\n\n\n"));
      finalMessages.add(
          ChatCompletionMessageParam.ofSystem(
              ChatCompletionSystemMessageParam.builder().content(joinedSystemMessage).build()));
    }
    finalMessages.addAll(messages);
    return finalMessages;
  }

  public ChatCompletionCreateParams build() {
    List<ChatCompletionMessageParam> finalMessages = new ArrayList<>();
    if (!overallSystemMessages.isEmpty()) {
      String joinedSystemMessage =
          overallSystemMessages.stream().collect(Collectors.joining("\n\n\n"));
      finalMessages.add(
          ChatCompletionMessageParam.ofSystem(
              ChatCompletionSystemMessageParam.builder().content(joinedSystemMessage).build()));
    }
    finalMessages.addAll(messages);
    ChatCompletionCreateParams.Builder requestBuilder = builder.messages(finalMessages).n(1L);
    return requestBuilder.build();
  }

  public OpenAIChatRequestBuilder addToOverallSystemMessage(String message) {
    overallSystemMessages.add(message);
    return this;
  }

  public OpenAIChatRequestBuilder addUserMessage(String message) {
    messages.add(
        ChatCompletionMessageParam.ofUser(
            ChatCompletionUserMessageParam.builder().content(message).build()));
    return this;
  }
}

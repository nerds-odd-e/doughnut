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
      "This is a PKM system: wiki-style Markdown notes in notebooks, with [[wiki links]] between notes.";
  public final List<ChatCompletionMessageParam> messages = new ArrayList<>();
  private final List<String> overallSystemMessages = new ArrayList<>();
  ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder();

  public static OpenAIChatRequestBuilder chatAboutNoteRequestBuilder(String modelName, Note note) {
    return chatAboutNoteRequestBuilder(modelName, note.getNoteDescription());
  }

  /**
   * @param focusNoteContextBlock Markdown from {@link
   *     com.odde.doughnut.services.focusContext.FocusContextMarkdownRenderer#render(com.odde.doughnut.services.focusContext.FocusContextResult,
   *     com.odde.doughnut.services.focusContext.RetrievalConfig)} or {@link
   *     Note#getNoteDescription()}, placed in the overall system message.
   */
  public static OpenAIChatRequestBuilder chatAboutNoteRequestBuilder(
      String modelName, String focusNoteContextBlock) {
    return new OpenAIChatRequestBuilder()
        .model(modelName)
        .addToOverallSystemMessage(systemInstruction)
        .addToOverallSystemMessage(focusNoteContextBlock);
  }

  public OpenAIChatRequestBuilder model(String modelName) {
    builder.model(ChatModel.of(modelName));
    return this;
  }

  public OpenAIChatRequestBuilder responseJsonSchema(InstructionAndSchema tool) {
    addToOverallSystemMessage(tool.getMessageBody());
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
    return builder.messages(buildMessages()).n(1L).build();
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

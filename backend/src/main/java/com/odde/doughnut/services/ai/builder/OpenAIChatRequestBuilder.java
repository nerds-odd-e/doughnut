package com.odde.doughnut.services.ai.builder;

import com.odde.doughnut.entities.Note;
import com.openai.models.ChatModel;
import com.openai.models.ReasoningEffort;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionDeveloperMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OpenAIChatRequestBuilder {
  public static final String systemInstruction =
      "This is a PKM system: wiki-style Markdown notes in notebooks, with [[wiki links]] between notes.";
  public final List<ChatCompletionMessageParam> messages = new ArrayList<>();
  private final List<String> overallSystemMessages = new ArrayList<>();
  private ReasoningEffort reasoningEffort = ReasoningEffort.NONE;
  private long maxCompletionTokens = 700L;
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

  public List<ChatCompletionMessageParam> buildMessages() {
    List<ChatCompletionMessageParam> finalMessages = new ArrayList<>();
    if (!overallSystemMessages.isEmpty()) {
      String joinedSystemMessage =
          overallSystemMessages.stream().collect(Collectors.joining("\n\n\n"));
      finalMessages.add(
          ChatCompletionMessageParam.ofDeveloper(
              ChatCompletionDeveloperMessageParam.builder().content(joinedSystemMessage).build()));
    }
    finalMessages.addAll(messages);
    return finalMessages;
  }

  public ChatCompletionCreateParams build() {
    return builder
        .messages(buildMessages())
        .n(1L)
        .reasoningEffort(reasoningEffort)
        .verbosity(ChatCompletionCreateParams.Verbosity.LOW)
        .maxCompletionTokens(maxCompletionTokens)
        .build();
  }

  public OpenAIChatRequestBuilder reasoningEffort(ReasoningEffort effort) {
    reasoningEffort = effort;
    return this;
  }

  public OpenAIChatRequestBuilder maxCompletionTokens(long tokens) {
    maxCompletionTokens = tokens;
    return this;
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

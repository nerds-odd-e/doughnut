package com.odde.doughnut.services.ai.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.tools.AiToolList;
import com.theokanning.openai.completion.chat.*;
import com.theokanning.openai.function.FunctionDefinition;
import java.util.ArrayList;
import java.util.List;

public class OpenAIChatRequestBuilder {
  public static final String systemInstruction =
      "This is a PKM system using hierarchical notes, each with a title and details, to capture atomic concepts.";
  public final List<ChatMessage> messages = new ArrayList<>();
  private final List<ChatTool> chatTools = new ArrayList<>();
  ChatCompletionRequest.ChatCompletionRequestBuilder builder = ChatCompletionRequest.builder();

  public static OpenAIChatRequestBuilder chatAboutNoteRequestBuilder(String modelName, Note note) {
    return new OpenAIChatRequestBuilder()
        .model(modelName)
        .addSystemMessage(systemInstruction)
        .addSystemMessage(note.getNoteDescription());
  }

  public OpenAIChatRequestBuilder model(String modelName) {
    builder.model(modelName);
    return this;
  }

  public OpenAIChatRequestBuilder maxTokens(int maxTokens) {
    builder.maxTokens(maxTokens);
    return this;
  }

  public OpenAIChatRequestBuilder addTool(AiToolList tool) {
    this.chatTools.addAll(tool.getFunctions().values().stream().map(ChatTool::new).toList());
    addUserMessage(tool.getMessageBody());
    return this;
  }

  public OpenAIChatRequestBuilder responseJsonSchema(AiToolList tool) {
    addUserMessage(tool.getMessageBody());
    FunctionDefinition schemaDefinition = tool.getFunctions().values().stream().findFirst().get();
    ResponseJsonSchema jsonSchema =
        ResponseJsonSchema.builder()
            .name(schemaDefinition.getName())
            .schemaDefinition(schemaDefinition.getParametersDefinition())
            .schemaClass((Class<Object>) schemaDefinition.getParametersDefinitionClass())
            .strict(true)
            .build();
    ChatResponseFormat respFormat = ChatResponseFormat.jsonSchema(jsonSchema);
    builder.responseFormat(respFormat);
    return this;
  }

  public List<ChatMessage> buildMessages() {
    return messages;
  }

  public ChatCompletionRequest build() {
    ChatCompletionRequest.ChatCompletionRequestBuilder requestBuilder =
        builder.messages(messages).stream(false).n(1);
    if (!chatTools.isEmpty()) {
      requestBuilder.tools(chatTools);
    }
    return requestBuilder.build();
  }

  public OpenAIChatRequestBuilder addSystemMessage(String message) {
    messages.add(new SystemMessage(message));
    return this;
  }

  public OpenAIChatRequestBuilder addUserMessage(String message) {
    messages.add(new UserMessage(message));
    return this;
  }

  public OpenAIChatRequestBuilder addFunctionCallMessage(
      Object arguments, String evaluateQuestion) {
    AssistantMessage msg = new AssistantMessage(null);
    ChatFunctionCall chatFunctionCall =
        new ChatFunctionCall(evaluateQuestion, new ObjectMapper().valueToTree(arguments));
    ChatToolCall call = new ChatToolCall();
    call.setFunction(chatFunctionCall);
    msg.setToolCalls(List.of(call));
    messages.add(msg);
    return this;
  }
}

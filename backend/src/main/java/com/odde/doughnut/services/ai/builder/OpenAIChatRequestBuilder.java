package com.odde.doughnut.services.ai.builder;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.tools.FunctionDefinition;
import com.odde.doughnut.services.ai.tools.InstructionAndSchema;
import com.theokanning.openai.completion.chat.*;
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

  public OpenAIChatRequestBuilder responseJsonSchema(InstructionAndSchema tool) {
    addUserMessage(tool.getMessageBody());
    FunctionDefinition schemaDefinition = tool.getFunctionDefinition();
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
}

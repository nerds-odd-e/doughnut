package com.odde.doughnut.services.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import java.util.List;
import java.util.Optional;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class ChatMessageForFineTuning {

  @NonNull String role;
  String content;

  @JsonProperty("function_call")
  ChatFunctionCallForFineTuning functionCall;

  private static final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

  public static ChatMessageForFineTuning from(ChatCompletionMessageParam messageParam) {
    var chatMessageForFineTuning = new ChatMessageForFineTuning();

    if (messageParam.system().isPresent()) {
      ChatCompletionSystemMessageParam systemMsg = messageParam.system().get();
      chatMessageForFineTuning.role = "system";
      chatMessageForFineTuning.content = extractContentString(systemMsg.content());
    } else if (messageParam.user().isPresent()) {
      ChatCompletionUserMessageParam userMsg = messageParam.user().get();
      chatMessageForFineTuning.role = "user";
      chatMessageForFineTuning.content = extractContentString(userMsg.content());
    } else if (messageParam.assistant().isPresent()) {
      ChatCompletionAssistantMessageParam assistantMsg = messageParam.assistant().get();
      chatMessageForFineTuning.role = "assistant";
      chatMessageForFineTuning.content =
          assistantMsg.content().map(ChatMessageForFineTuning::extractContentString).orElse(null);

      // Extract function call from tool calls if present
      Optional<List<ChatCompletionMessageToolCall>> toolCallsOpt = assistantMsg.toolCalls();
      if (toolCallsOpt.isPresent() && !toolCallsOpt.get().isEmpty()) {
        ChatCompletionMessageToolCall toolCall = toolCallsOpt.get().get(0);
        if (toolCall.function().isPresent()) {
          ChatCompletionMessageFunctionToolCall functionToolCall = toolCall.asFunction();
          chatMessageForFineTuning.functionCall =
              ChatFunctionCallForFineTuning.from(functionToolCall.function());
        }
      }
    }

    return chatMessageForFineTuning;
  }

  public static String extractContentString(Object contentObj) {
    if (contentObj == null) {
      return null;
    }
    if (contentObj instanceof String) {
      return (String) contentObj;
    }
    // For Content objects, try to extract string value using Jackson tree model
    try {
      com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.valueToTree(contentObj);
      if (jsonNode.isTextual()) {
        return jsonNode.asText();
      }
      // If it's an object, try to find a "text" or "content" field
      if (jsonNode.isObject()) {
        if (jsonNode.has("text")) {
          return jsonNode.get("text").asText();
        }
        if (jsonNode.has("content")) {
          return jsonNode.get("content").asText();
        }
      }
      // Fallback: serialize to JSON string and unquote if needed
      String json = objectMapper.writeValueAsString(contentObj);
      if (json.startsWith("\"") && json.endsWith("\"")) {
        return json.substring(1, json.length() - 1);
      }
      return json;
    } catch (Exception e) {
      // Fallback to toString if serialization fails
      String str = contentObj.toString();
      // If toString returns a quoted string, unquote it
      if (str.startsWith("\"") && str.endsWith("\"")) {
        return str.substring(1, str.length() - 1);
      }
      return str;
    }
  }
}

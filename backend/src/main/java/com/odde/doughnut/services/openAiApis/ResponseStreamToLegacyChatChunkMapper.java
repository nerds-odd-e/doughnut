package com.odde.doughnut.services.openAiApis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseStreamEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Maps OpenAI Responses stream events to SSE chunk JSON strings consumed by the frontend. */
public final class ResponseStreamToLegacyChatChunkMapper {
  private static final String FINISH_STOP = "stop";
  private static final String FINISH_TOOL_CALLS = "tool_calls";

  private final ObjectMapper objectMapper;
  private String activeToolName;
  private String activeToolCallId;
  private boolean toolCallOpened;

  public ResponseStreamToLegacyChatChunkMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public List<String> map(ResponseStreamEvent event) throws JsonProcessingException {
    List<String> out = new ArrayList<>();
    if (event.isError()) {
      throw new RuntimeException(event.asError().message());
    }
    if (event.isFailed()) {
      throw new RuntimeException("OpenAI response stream failed");
    }
    if (event.isIncomplete()) {
      throw new RuntimeException("OpenAI response stream incomplete");
    }
    if (event.isOutputTextDelta()) {
      String delta = event.asOutputTextDelta().delta();
      if (delta != null && !delta.isEmpty()) {
        out.add(textDeltaChunk(delta));
      }
    } else if (event.isOutputTextDone()) {
      out.add(finishTextChunk());
    } else if (event.isOutputItemAdded()) {
      ResponseOutputItem item = event.asOutputItemAdded().item();
      if (item.isFunctionCall()) {
        ResponseFunctionToolCall fc = item.asFunctionCall();
        activeToolName = fc.name();
        activeToolCallId = fc.id().orElse(fc.callId());
        toolCallOpened = false;
      }
    } else if (event.isFunctionCallArgumentsDelta()) {
      String frag = event.asFunctionCallArgumentsDelta().delta();
      if (frag != null && !frag.isEmpty()) {
        if (!toolCallOpened && activeToolName != null) {
          out.add(toolArgumentsChunk(Optional.of(activeToolName), frag, true));
          toolCallOpened = true;
        } else {
          out.add(toolArgumentsChunk(Optional.empty(), frag, false));
        }
      }
    } else if (event.isFunctionCallArgumentsDone()) {
      var done = event.asFunctionCallArgumentsDone();
      String name = done.name() != null ? done.name() : activeToolName;
      String args = done.arguments();
      if (!toolCallOpened) {
        out.add(toolArgumentsChunk(Optional.ofNullable(name), args != null ? args : "", true));
        toolCallOpened = true;
      }
      out.add(finishToolCallsChunk());
      activeToolName = null;
      activeToolCallId = null;
      toolCallOpened = false;
    }
    return out;
  }

  private String textDeltaChunk(String content) throws JsonProcessingException {
    ObjectNode delta = objectMapper.createObjectNode();
    delta.put("content", content);
    return chunkJson(delta, Optional.empty());
  }

  private String finishTextChunk() throws JsonProcessingException {
    return chunkJson(objectMapper.createObjectNode(), Optional.of(FINISH_STOP));
  }

  private String toolArgumentsChunk(
      Optional<String> name, String argumentsFragment, boolean includeMeta)
      throws JsonProcessingException {
    ObjectNode function = objectMapper.createObjectNode();
    name.ifPresent(n -> function.put("name", n));
    function.put("arguments", argumentsFragment);

    ObjectNode toolCall = objectMapper.createObjectNode();
    toolCall.put("index", 0);
    toolCall.put("type", "function");
    toolCall.set("function", function);
    if (includeMeta && activeToolCallId != null) {
      toolCall.put("id", activeToolCallId);
    }

    ArrayNode toolCalls = objectMapper.createArrayNode();
    toolCalls.add(toolCall);

    ObjectNode delta = objectMapper.createObjectNode();
    delta.set("tool_calls", toolCalls);
    return chunkJson(delta, Optional.empty());
  }

  private String finishToolCallsChunk() throws JsonProcessingException {
    return chunkJson(objectMapper.createObjectNode(), Optional.of(FINISH_TOOL_CALLS));
  }

  private String chunkJson(ObjectNode delta, Optional<String> finishReason)
      throws JsonProcessingException {
    ObjectNode choice = objectMapper.createObjectNode();
    choice.set("delta", delta);
    finishReason.ifPresentOrElse(
        reason -> choice.put("finish_reason", reason), () -> choice.putNull("finish_reason"));

    ObjectNode root = objectMapper.createObjectNode();
    ArrayNode choices = objectMapper.createArrayNode();
    choices.add(choice);
    root.set("choices", choices);

    return objectMapper.writeValueAsString(root);
  }
}

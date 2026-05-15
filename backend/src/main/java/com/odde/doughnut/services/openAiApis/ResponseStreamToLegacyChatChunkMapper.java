package com.odde.doughnut.services.openAiApis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.services.GlobalSettingsService;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionChunk.Choice;
import com.openai.models.chat.completions.ChatCompletionChunk.Choice.Delta;
import com.openai.models.chat.completions.ChatCompletionChunk.Choice.Delta.ToolCall;
import com.openai.models.chat.completions.ChatCompletionChunk.Choice.Delta.ToolCall.Function;
import com.openai.models.chat.completions.ChatCompletionChunk.Choice.Delta.ToolCall.Type;
import com.openai.models.chat.completions.ChatCompletionChunk.Choice.FinishReason;
import com.openai.models.responses.ResponseFunctionToolCall;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseStreamEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ResponseStreamToLegacyChatChunkMapper {
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
    Delta delta = Delta.builder().content(content).build();
    return chunkJson(delta, Optional.empty());
  }

  private String finishTextChunk() throws JsonProcessingException {
    Delta delta = Delta.builder().build();
    return chunkJson(delta, Optional.of(FinishReason.STOP));
  }

  private String toolArgumentsChunk(
      Optional<String> name, String argumentsFragment, boolean includeMeta)
      throws JsonProcessingException {
    Function.Builder fn = Function.builder();
    name.ifPresent(fn::name);
    fn.arguments(argumentsFragment);
    ToolCall.Builder tc = ToolCall.builder().index(0L).type(Type.FUNCTION).function(fn.build());
    if (includeMeta && activeToolCallId != null) {
      tc.id(activeToolCallId);
    }
    Delta delta = Delta.builder().addToolCall(tc.build()).build();
    return chunkJson(delta, Optional.empty());
  }

  private String finishToolCallsChunk() throws JsonProcessingException {
    Delta delta = Delta.builder().build();
    return chunkJson(delta, Optional.of(FinishReason.TOOL_CALLS));
  }

  private String chunkJson(Delta delta, Optional<FinishReason> finishReason)
      throws JsonProcessingException {
    Choice.Builder choiceBuilder =
        Choice.builder().index(0L).delta(delta).logprobs(Optional.empty());
    choiceBuilder.finishReason(finishReason);
    Choice choice = choiceBuilder.build();
    ChatCompletionChunk chunk =
        ChatCompletionChunk.builder()
            .id("chatcmpl-responses")
            .created(System.currentTimeMillis() / 1000L)
            .model(GlobalSettingsService.DEFAULT_CHAT_MODEL)
            .choices(List.of(choice))
            .build();
    return objectMapper.writeValueAsString(chunk);
  }
}

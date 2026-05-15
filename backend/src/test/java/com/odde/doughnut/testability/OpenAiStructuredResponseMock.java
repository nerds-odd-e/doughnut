package com.odde.doughnut.testability;

import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.services.GlobalSettingsService;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputText;
import com.openai.models.responses.ResponseStatus;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.models.responses.ToolChoiceOptions;
import com.openai.services.blocking.ResponseService;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class OpenAiStructuredResponseMock {
  private final ResponseService responseService;
  private final ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
  private final Map<Class<?>, Queue<Object>> structuredResults = new HashMap<>();
  private final Queue<Object> fallbackStructuredResults = new LinkedList<>();

  private record MalformedStructuredContent(String content) {}

  public OpenAiStructuredResponseMock(OpenAIClient officialClient) {
    this.responseService = Mockito.mock(ResponseService.class);
    Mockito.when(officialClient.responses()).thenReturn(responseService);
    Mockito.when(responseService.create(ArgumentMatchers.any(StructuredResponseCreateParams.class)))
        .thenAnswer(
            invocation -> {
              StructuredResponseCreateParams<?> params = invocation.getArgument(0);
              Object result = pollStructuredResult(params.responseType());
              if (result == null) {
                return null;
              }
              String content =
                  result instanceof MalformedStructuredContent malformed
                      ? malformed.content()
                      : objectMapperConfig.objectMapper().valueToTree(result).toString();
              return new StructuredResponse(
                  params.responseType(), buildStructuredResponse(content));
            });
  }

  public ResponseService responseService() {
    return responseService;
  }

  /** Stubs the next {@code officialClient.responses().create(...)} result. */
  public void stubStructuredResponse(Object result) {
    if (result == null) {
      fallbackStructuredResults.add(null);
    } else {
      addStructuredResult(result);
    }
  }

  public void stubStructuredResponseMalformed(String malformedJson) {
    fallbackStructuredResults.add(new MalformedStructuredContent(malformedJson));
  }

  private void addStructuredResult(Object result) {
    Queue<Object> resultsForType =
        structuredResults.computeIfAbsent(result.getClass(), ignored -> new LinkedList<>());
    resultsForType.clear();
    resultsForType.add(result);
  }

  private Object pollStructuredResult(Class<?> responseType) {
    Queue<Object> exactQueue = structuredResults.get(responseType);
    if (exactQueue != null && !exactQueue.isEmpty()) {
      return exactQueue.poll();
    }
    Optional<Queue<Object>> assignableQueue =
        structuredResults.entrySet().stream()
            .filter(entry -> responseType.isAssignableFrom(entry.getKey()))
            .map(Map.Entry::getValue)
            .filter(queue -> !queue.isEmpty())
            .findFirst();
    if (assignableQueue.isPresent()) {
      return assignableQueue.get().poll();
    }
    Optional<Queue<Object>> supertypeQueue =
        structuredResults.entrySet().stream()
            .filter(entry -> entry.getKey().isAssignableFrom(responseType))
            .map(Map.Entry::getValue)
            .filter(queue -> !queue.isEmpty())
            .findFirst();
    if (supertypeQueue.isPresent()) {
      return supertypeQueue.get().poll();
    }
    return fallbackStructuredResults.poll();
  }

  private Response buildStructuredResponse(String content) {
    ResponseOutputText outputText =
        ResponseOutputText.builder().annotations(List.of()).text(content).build();
    ResponseOutputMessage message =
        ResponseOutputMessage.builder()
            .id("msg-mock")
            .addContent(outputText)
            .status(ResponseOutputMessage.Status.COMPLETED)
            .build();
    return Response.builder()
        .id("resp-mock")
        .createdAt((double) (System.currentTimeMillis() / 1000L))
        .error(Optional.empty())
        .incompleteDetails(Optional.empty())
        .instructions(Optional.empty())
        .metadata(Response.Metadata.builder().build())
        .model(GlobalSettingsService.DEFAULT_CHAT_MODEL)
        .output(List.of(ResponseOutputItem.ofMessage(message)))
        .parallelToolCalls(false)
        .temperature(Optional.empty())
        .toolChoice(ToolChoiceOptions.NONE)
        .tools(List.of())
        .topP(Optional.empty())
        .status(ResponseStatus.COMPLETED)
        .build();
  }
}

package com.odde.doughnut.testability;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import com.theokanning.openai.client.OpenAiApi;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import retrofit2.Call;

public class OpenAIChatCompletionStreamMocker {
  private final OpenAiApi openAiApi;
  private final List<String> messageDeltas = new ArrayList<>();

  public OpenAIChatCompletionStreamMocker(OpenAiApi openAiApi) {
    this.openAiApi = openAiApi;
  }

  public OpenAIChatCompletionStreamMocker withMessage(String message) {
    // Split message into chunks for streaming
    messageDeltas.add(message);
    return this;
  }

  public void mockStreamResponse() {
    // Build SSE response
    StringBuilder sseResponse = new StringBuilder();

    // Add chunks for each message
    for (String message : messageDeltas) {
      // Send a simple delta chunk
      String chunkJson =
          String.format(
              "{\"choices\":[{\"index\":0,\"delta\":{\"content\":\"%s\",\"role\":\"assistant\"},\"finish_reason\":null}]}",
              escapeJson(message));
      sseResponse.append("data: ").append(chunkJson).append("\n\n");
    }

    // Send final done chunk
    sseResponse.append(
        "data: {\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n");
    sseResponse.append("data: [DONE]\n\n");

    ResponseBody responseBody =
        ResponseBody.create(MediaType.parse("text/event-stream"), sseResponse.toString());
    Call<ResponseBody> call = new ResponseBodyCallStub(responseBody);

    doReturn(call).when(openAiApi).createChatCompletionStream(any());
  }

  private String escapeJson(String message) {
    return message
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}

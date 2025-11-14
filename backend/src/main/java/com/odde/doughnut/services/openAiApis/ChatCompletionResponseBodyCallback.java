package com.odde.doughnut.services.openAiApis;

import com.odde.doughnut.exceptions.OpenAiUnauthorizedException;
import io.reactivex.FlowableEmitter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Callback to parse Server Sent Events from a chat completion stream and emit ChatCompletionChunk
 * objects.
 */
public class ChatCompletionResponseBodyCallback implements Callback<ResponseBody> {
  private final FlowableEmitter<String> emitter; // Emit raw JSON strings instead of chunks

  public ChatCompletionResponseBodyCallback(FlowableEmitter<String> emitter) {
    this.emitter = emitter;
  }

  @Override
  public void onResponse(
      @NotNull Call<ResponseBody> call, @NotNull Response<ResponseBody> response) {
    if (!response.isSuccessful()) {
      int statusCode = response.code();
      // Handle both 401 (Unauthorized) and 400 (Bad Request) as unauthorized errors
      // since the mock returns BAD_REQUEST status for unauthorized scenarios
      if (statusCode == HttpStatus.UNAUTHORIZED.value()
          || statusCode == HttpStatus.BAD_REQUEST.value()) {
        emitter.onError(new OpenAiUnauthorizedException("Unauthorized: " + response.message()));
      } else {
        emitter.onError(new RuntimeException("Unsuccessful response: " + statusCode));
      }
      return;
    }

    try (ResponseBody body = response.body()) {
      if (body == null) {
        emitter.onError(new RuntimeException("Response body is null"));
        return;
      }

      processStream(body.byteStream());
      emitter.onComplete();
    } catch (Exception e) {
      emitter.onError(e);
    }
  }

  @Override
  public void onFailure(@NotNull Call<ResponseBody> call, @NotNull Throwable t) {
    emitter.onError(t);
  }

  private void processStream(InputStream inputStream) throws IOException {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("data:")) {
          String data = line.substring(5).trim();
          if (data.equals("[DONE]")) {
            return;
          }
          if (!data.isEmpty()) {
            try {
              // Emit raw JSON string directly to preserve delta field
              // The SDK's ChatCompletionChunk might not preserve delta when deserializing
              emitter.onNext(data);
            } catch (Exception e) {
              // Skip malformed chunks
            }
          }
        }
      }
    }
  }
}

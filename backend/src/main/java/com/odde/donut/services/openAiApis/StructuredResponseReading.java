package com.odde.donut.services.openAiApis;

import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseStatus;
import com.openai.models.responses.StructuredResponse;
import java.util.Optional;

public record StructuredResponseReading<T>(Optional<T> value, Optional<String> incompleteReason) {
  public static <T> StructuredResponseReading<T> read(StructuredResponse<T> response) {
    if (response == null) {
      return empty();
    }
    if (response.status().filter(ResponseStatus.INCOMPLETE::equals).isPresent()) {
      return new StructuredResponseReading<>(Optional.empty(), incompleteReason(response));
    }
    if (response.output() == null || response.output().isEmpty()) {
      return empty();
    }
    return new StructuredResponseReading<>(firstOutput(response), Optional.empty());
  }

  private static <T> StructuredResponseReading<T> empty() {
    return new StructuredResponseReading<>(Optional.empty(), Optional.empty());
  }

  private static <T> Optional<String> incompleteReason(StructuredResponse<T> response) {
    return response
        .incompleteDetails()
        .flatMap(Response.IncompleteDetails::reason)
        .map(Response.IncompleteDetails.Reason::asString);
  }

  private static <T> Optional<T> firstOutput(StructuredResponse<T> response) {
    return response.output().stream()
        .flatMap(output -> output.message().stream())
        .flatMap(message -> message.content().stream())
        .flatMap(content -> content.outputText().stream())
        .findFirst();
  }
}

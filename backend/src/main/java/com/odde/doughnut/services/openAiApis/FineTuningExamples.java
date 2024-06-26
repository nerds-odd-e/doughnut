package com.odde.doughnut.services.openAiApis;

import com.odde.doughnut.exceptions.OpenAIServiceErrorException;
import com.odde.doughnut.services.ai.OpenAIChatGPTFineTuningExample;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;

public record FineTuningExamples(List<OpenAIChatGPTFineTuningExample> examples) {

  public String toJsonL() {
    if (examples.size() < 10) {
      throw new OpenAIServiceErrorException(
          "Positive feedback cannot be less than 10.", HttpStatus.BAD_REQUEST);
    }
    return examples.stream()
        .map(OpenAIChatGPTFineTuningExample::toJsonString)
        .collect(Collectors.joining("\n"));
  }
}

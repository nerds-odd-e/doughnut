package com.odde.doughnut.controllers.json;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import lombok.*;

@NoArgsConstructor
@Data
public class AiCompletionParams {
  private String detailsToComplete;

  @JsonIgnore
  public String getCompletionPrompt() {
    HashMap<String, String> arguments = new HashMap<>();
    arguments.put("details_to_complete", detailsToComplete == null ? "" : detailsToComplete);
    return ("Please complete the concise details of the note of focus. Keep it short."
            + " Don't make assumptions about the context. Ask for clarification through tool function if my request is ambiguous."
            + " The current details in JSON format are: \n%s")
        .formatted(defaultObjectMapper().valueToTree(arguments).toPrettyString());
  }
}

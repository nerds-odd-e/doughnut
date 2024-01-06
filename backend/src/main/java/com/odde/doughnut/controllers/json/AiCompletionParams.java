package com.odde.doughnut.controllers.json;

import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.*;
import org.springframework.lang.Nullable;

@NoArgsConstructor
@Data
public class AiCompletionParams {
  @Nullable private String threadId;
  private String detailsToComplete;
  private List<ClarifyingQuestionAndAnswer> clarifyingQuestionAndAnswers = new ArrayList<>();

  public String getDetailsToComplete() {
    if (detailsToComplete == null) {
      return "";
    }
    return detailsToComplete;
  }

  @JsonIgnore
  public String getCompletionPrompt() {
    HashMap<String, String> arguments = new HashMap<>();
    arguments.put("details_to_complete", getDetailsToComplete());
    return ("Please complete the concise details of the note of focus. Keep it short."
            + " Don't make assumptions about the context. Ask for clarification through tool function if my request is ambiguous."
            + " The current details in JSON format are: \n%s")
        .formatted(defaultObjectMapper().valueToTree(arguments).toPrettyString());
  }

  @AllArgsConstructor
  public static class UserResponseToClarifyingQuestion {
    public String answerFromUser;
  }
}

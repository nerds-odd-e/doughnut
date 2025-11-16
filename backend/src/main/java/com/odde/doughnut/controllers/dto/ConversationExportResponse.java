package com.odde.doughnut.controllers.dto;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.lang.NonNull;

@Data
@AllArgsConstructor
public class ConversationExportResponse {
  @NonNull private final ChatCompletionRequest request;
  @NonNull private final String title;
}

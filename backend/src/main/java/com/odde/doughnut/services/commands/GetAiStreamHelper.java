package com.odde.doughnut.services.commands;

import com.odde.doughnut.entities.*;
import java.util.List;

public class GetAiStreamHelper {
  public static String formatUnsentMessages(List<ConversationMessage> messages) {
    StringBuilder combined = new StringBuilder();
    for (ConversationMessage msg : messages) {
      combined.append(String.format("user `%s` says:%n", msg.getSender().getName()));
      combined.append("-----------------\n");
      combined.append(msg.getMessage());
      combined.append("\n\n");
    }
    return combined.toString();
  }
}

package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.Answer;
import com.odde.doughnut.entities.RecallLog;
import com.odde.doughnut.entities.RecallPrompt;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RecallHistoryItem {
  private RecallLog recallLog;

  private RecallPromptHistoryItem recallPrompt;

  public static List<RecallHistoryItem> from(List<RecallLog> logs, List<RecallPrompt> prompts) {
    Map<Integer, RecallPrompt> promptsByAnswerId = new HashMap<>();
    for (RecallPrompt prompt : prompts) {
      Answer answer = prompt.getAnswer();
      if (answer != null && answer.getId() != null) {
        promptsByAnswerId.put(answer.getId(), prompt);
      }
    }
    Set<Integer> pairedPromptIds = new HashSet<>();
    List<RecallHistoryItem> history = new ArrayList<>();
    for (RecallLog log : logs) {
      RecallPrompt prompt = promptFor(log, promptsByAnswerId);
      history.add(from(log, prompt));
      if (prompt != null) {
        pairedPromptIds.add(prompt.getId());
      }
    }
    for (RecallPrompt prompt : prompts) {
      if (!pairedPromptIds.contains(prompt.getId())) {
        history.add(from(null, prompt));
      }
    }
    history.sort(
        Comparator.comparing(RecallHistoryItem::occurredAt, Comparator.reverseOrder())
            .thenComparing(RecallHistoryItem::sortId, Comparator.reverseOrder()));
    return history;
  }

  static RecallHistoryItem from(RecallLog recallLog, RecallPrompt recallPrompt) {
    RecallHistoryItem item = new RecallHistoryItem();
    item.setRecallLog(recallLog);
    if (recallPrompt != null) {
      item.setRecallPrompt(RecallPromptHistoryItem.from(recallPrompt));
    }
    return item;
  }

  private Timestamp occurredAt() {
    if (recallLog != null) {
      return recallLog.getRecordedAt();
    }
    return recallPrompt.getQuestionGeneratedTime();
  }

  private Integer sortId() {
    if (recallLog != null) {
      return recallLog.getId();
    }
    return recallPrompt.getId();
  }

  private static RecallPrompt promptFor(
      RecallLog log, Map<Integer, RecallPrompt> promptsByAnswerId) {
    Answer answer = log.getAnswer();
    if (answer == null || answer.getId() == null) {
      return null;
    }
    return promptsByAnswerId.get(answer.getId());
  }
}

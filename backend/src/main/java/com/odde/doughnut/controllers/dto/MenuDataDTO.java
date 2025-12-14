package com.odde.doughnut.controllers.dto;

import com.odde.doughnut.entities.ConversationMessage;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MenuDataDTO {
  private AssimilationCountDTO assimilationCount;
  private DueMemoryTrackers recallStatus;
  private List<ConversationMessage> unreadConversations;
}

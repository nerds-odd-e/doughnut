package com.odde.donut.controllers.dto;

import com.odde.donut.entities.ConversationMessage;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MenuDataDTO {
  private AssimilationCountDTO assimilationCount;
  private DueMemoryTrackers recallStatus;
  private List<ConversationMessage> unreadMessages;
}

package com.odde.doughnut.controllers.dto;

import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserForListing {
  private Integer id;
  private String name;
  private long noteCount;
  private long memoryTrackerCount;
  private Timestamp lastNoteTime;
  private Timestamp lastAssimilationTime;
  private Timestamp lastRecallTime;
}

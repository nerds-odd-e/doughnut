package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

public class NoteStatistics {
  @Getter @Setter private ReviewPoint reviewPoint;
  @Getter @Setter private NoteRealm note;
  @Getter @Setter private Timestamp createdAt;
  @Getter @Setter public ReviewSetting reviewSetting;
}

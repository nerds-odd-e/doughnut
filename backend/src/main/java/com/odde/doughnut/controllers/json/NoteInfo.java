package com.odde.doughnut.controllers.json;

import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;

public class NoteInfo {
  @Getter @Setter private ReviewPoint reviewPoint;
  @NotNull @Getter @Setter private NoteRealm note;
  @NotNull @Getter @Setter private Timestamp createdAt;
  @Getter @Setter public ReviewSetting reviewSetting;
}

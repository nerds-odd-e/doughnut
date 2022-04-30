package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.ReviewPoint;
import com.odde.doughnut.entities.ReviewSetting;
import java.util.Optional;
import javax.validation.Valid;

public class InitialInfo {
  @Valid public ReviewPoint reviewPoint;
  public Optional<Integer> noteId;
  public Optional<Integer> linkId;
  @Valid public ReviewSetting reviewSetting;
}

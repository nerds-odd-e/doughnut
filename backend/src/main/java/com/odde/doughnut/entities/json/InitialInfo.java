package com.odde.doughnut.entities.json;

import com.odde.doughnut.entities.ReviewSetting;
import javax.validation.Valid;

public class InitialInfo {
  public Integer thingId;
  public Boolean skipReview;
  @Valid public ReviewSetting reviewSetting;
}

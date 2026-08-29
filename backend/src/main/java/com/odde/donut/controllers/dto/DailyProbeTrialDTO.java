package com.odde.donut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailyProbeTrialDTO {
  public String stimulus;
  public String response;
  public Integer rtMs;
  public Boolean correct;
}

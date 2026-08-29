package com.odde.donut.controllers.dto;

import java.util.List;

public class DailyProbeRequestDTO {
  public List<DailyProbeTrialDTO> trials;
  public Double speed;
  public Integer accuracy;
  public Integer lapseCount;
  public Double variability;
}

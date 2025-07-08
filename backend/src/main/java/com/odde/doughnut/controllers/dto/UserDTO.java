package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {

  @NotNull
  @Size(min = 1, max = 100)
  @Getter
  @Setter
  private String name;

  @Getter @Setter private Integer dailyAssimilationCount = 15;

  @Pattern(regexp = "^\\d+((,\\s*\\d+){1,1000})*$", message = "must be numbers separated by ','")
  @Getter
  @Setter
  private String spaceIntervals = "0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55";
}

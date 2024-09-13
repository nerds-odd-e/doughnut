package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class UserDTO {

  @NotNull
  @Size(min = 1, max = 100)
  @Getter
  @Setter
  private String name;

  @Getter @Setter private Integer dailyNewNotesCount = 15;

  @Pattern(regexp = "^\\d+((,\\s*\\d+){1,1000})*$", message = "must be numbers separated by ','")
  @Getter
  @Setter
  private String spaceIntervals = "0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55";
}

package com.odde.doughnut.controllers.json;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

public class CircleJoiningByInvitation {
  @NotNull
  @Size(min = 10, max = 20)
  @Getter
  @Setter
  String invitationCode;
}

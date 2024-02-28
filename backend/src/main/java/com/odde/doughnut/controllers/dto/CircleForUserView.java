package com.odde.doughnut.controllers.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class CircleForUserView {
  @NotNull @Setter @Getter Integer id;
  @NotNull @Setter @Getter String name;
  @NotNull @Setter @Getter String invitationCode;
  @NotNull @Setter @Getter NotebooksViewedByUser notebooks;
  @NotNull @Setter @Getter List<UserForOtherUserView> members;
}

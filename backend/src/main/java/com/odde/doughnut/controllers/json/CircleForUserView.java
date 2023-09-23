package com.odde.doughnut.controllers.json;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

public class CircleForUserView {
  @Setter @Getter Integer id;
  @Setter @Getter String name;
  @Setter @Getter String invitationCode;
  @Setter @Getter NotebooksViewedByUser notebooks;
  @Setter @Getter List<UserForOtherUserView> members;
}

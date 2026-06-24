package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TitleAliasInboundReferenceRewritePreviewDTO {

  private int referrerNoteId;
  private int targetNoteId;
  private String currentLinkInner;
  private String plannedLinkInner;
  private boolean visibleTextWillChange;
}

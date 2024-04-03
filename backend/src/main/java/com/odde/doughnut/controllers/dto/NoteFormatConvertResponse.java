package com.odde.doughnut.controllers.dto;

import lombok.Getter;
import lombok.Setter;

public class NoteFormatConvertResponse {

  @Getter @Setter private int resultCode;
  @Getter @Setter private String result;

  public NoteFormatConvertResponse(int resultCode, String result) {
    this.resultCode = resultCode;
    this.result = result;
  }
}

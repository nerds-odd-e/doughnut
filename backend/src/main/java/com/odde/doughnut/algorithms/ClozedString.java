package com.odde.doughnut.algorithms;

import org.apache.logging.log4j.util.Strings;

public class ClozedString {
  private ClozeDescription clozeDescription;
  private String cnt;

  public ClozedString(ClozeDescription clozeDescription, String cnt) {
    this.clozeDescription = clozeDescription;

    this.cnt = cnt;
  }

  @Override
  public String toString() {
    throw new RuntimeException("Not implemented, use `cloze` instead.");
  }

  public String cloze() {
    return cnt;
  }

  public boolean isPresent() {
    return Strings.isNotBlank(cnt);
  }
}

package com.odde.doughnut.services.focusContext;

import lombok.Getter;

@Getter
public class FocusContextFocusNote {
  private final String notebook;
  private final String title;
  private final String folderPath;
  private final String details;
  private final boolean detailsTruncated;

  public FocusContextFocusNote(
      String notebook, String title, String folderPath, String details, boolean detailsTruncated) {
    this.notebook = notebook;
    this.title = title;
    this.folderPath = folderPath;
    this.details = details;
    this.detailsTruncated = detailsTruncated;
  }
}

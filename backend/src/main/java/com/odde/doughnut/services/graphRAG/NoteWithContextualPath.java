package com.odde.doughnut.services.graphRAG;

import java.util.List;

public class NoteWithContextualPath extends BareNote {
  public final List<String> contextualPath;

  public NoteWithContextualPath(
      String uriAndTitle,
      String detailsTruncated,
      String parentUriAndTitle,
      String objectUriAndTitle,
      List<String> contextualPath) {
    super(uriAndTitle, detailsTruncated, parentUriAndTitle, objectUriAndTitle);
    this.contextualPath = contextualPath;
  }
}

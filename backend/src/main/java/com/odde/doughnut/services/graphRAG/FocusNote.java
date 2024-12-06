package com.odde.doughnut.services.graphRAG;

import java.util.List;

public class FocusNote extends BareNote {
  public final List<String> contextualPath;
  public final List<String> children;
  public final List<String> referrings;
  public final List<String> priorSiblings;
  public final List<String> youngerSiblings;

  public FocusNote(
      String uriAndTitle,
      String detailsTruncated,
      String parentUriAndTitle,
      String objectUriAndTitle,
      List<String> contextualPath,
      List<String> children,
      List<String> referrings,
      List<String> priorSiblings,
      List<String> youngerSiblings) {
    super(
        uriAndTitle,
        detailsTruncated,
        parentUriAndTitle,
        objectUriAndTitle,
        RelationshipToFocusNote.Itself);
    this.contextualPath = contextualPath;
    this.children = children;
    this.referrings = referrings;
    this.priorSiblings = priorSiblings;
    this.youngerSiblings = youngerSiblings;
  }
}

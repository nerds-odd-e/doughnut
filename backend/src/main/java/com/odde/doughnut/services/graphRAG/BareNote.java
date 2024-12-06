package com.odde.doughnut.services.graphRAG;

public class BareNote {
  public final String uriAndTitle;
  public final String detailsTruncated;
  public final String parentUriAndTitle;
  public final String objectUriAndTitle;
  public final RelationshipToFocusNote relationshipToFocusNote;

  public BareNote(
      String uriAndTitle,
      String detailsTruncated,
      String parentUriAndTitle,
      String objectUriAndTitle,
      RelationshipToFocusNote relationshipToFocusNote) {
    this.uriAndTitle = uriAndTitle;
    this.detailsTruncated = detailsTruncated;
    this.parentUriAndTitle = parentUriAndTitle;
    this.objectUriAndTitle = objectUriAndTitle;
    this.relationshipToFocusNote = relationshipToFocusNote;
  }
}

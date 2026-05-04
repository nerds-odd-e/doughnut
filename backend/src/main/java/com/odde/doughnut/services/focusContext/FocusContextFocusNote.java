package com.odde.doughnut.services.focusContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.sql.Timestamp;
import java.util.List;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class FocusContextFocusNote {
  private final String notebook;
  private final String title;
  private final String folderPath;
  private final int depth;
  private final List<String> outgoingLinks;
  private final List<String> inboundReferences;
  private final List<String> sampleSiblings;
  private final Timestamp createdAt;
  private final String details;
  private final boolean detailsTruncated;

  public FocusContextFocusNote(
      String notebook, String title, String folderPath, String details, boolean detailsTruncated) {
    this(
        notebook,
        title,
        folderPath,
        0,
        List.of(),
        List.of(),
        List.of(),
        null,
        details,
        detailsTruncated);
  }

  public FocusContextFocusNote(
      String notebook,
      String title,
      String folderPath,
      int depth,
      List<String> outgoingLinks,
      List<String> inboundReferences,
      List<String> sampleSiblings,
      Timestamp createdAt,
      String details,
      boolean detailsTruncated) {
    this.notebook = notebook;
    this.title = title;
    this.folderPath = folderPath;
    this.depth = depth;
    this.outgoingLinks = outgoingLinks;
    this.inboundReferences = inboundReferences;
    this.sampleSiblings = sampleSiblings;
    this.createdAt = createdAt;
    this.details = details;
    this.detailsTruncated = detailsTruncated;
  }
}

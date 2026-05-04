package com.odde.doughnut.services.focusContext;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.sql.Timestamp;
import java.util.List;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
public class FocusContextNote {
  private final String notebook;
  private final String title;
  private final String folderPath;
  private final int depth;
  private final List<String> retrievalPath;
  private final FocusContextEdgeType edgeType;
  private final Timestamp createdAt;
  private final String details;
  private final boolean detailsTruncated;

  public FocusContextNote(
      String notebook,
      String title,
      String folderPath,
      int depth,
      List<String> retrievalPath,
      FocusContextEdgeType edgeType,
      Timestamp createdAt,
      String details,
      boolean detailsTruncated) {
    this.notebook = notebook;
    this.title = title;
    this.folderPath = folderPath;
    this.depth = depth;
    this.retrievalPath = retrievalPath;
    this.edgeType = edgeType;
    this.createdAt = createdAt;
    this.details = details;
    this.detailsTruncated = detailsTruncated;
  }
}

package com.odde.doughnut.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.odde.doughnut.entities.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class ExportDataSchema {
  @JsonProperty("metadata")
  private Metadata metadata;

  @JsonProperty("notebooks")
  private List<NotebookExport> notebooks;

  @Data
  public static class Metadata {
    @JsonProperty("version")
    private String version = "1.0.0";

    @JsonProperty("exportedAt")
    private LocalDateTime exportedAt;

    @JsonProperty("exportedBy")
    private String exportedBy;
  }

  @Data
  public static class NotebookExport {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("shortDetails")
    private String shortDetails;

    @JsonProperty("certifiable")
    private boolean certifiable;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    @JsonProperty("settings")
    private NotebookSettings settings;

    @JsonProperty("notes")
    private List<NoteExport> notes;
  }

  @Data
  public static class NoteExport {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("details")
    private String details;

    @JsonProperty("wikidataId")
    private String wikidataId;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    @JsonProperty("links")
    private List<NoteLink> links;

    @JsonProperty("memoryStatus")
    private MemoryStatus memoryStatus;
  }

  @Data
  public static class NoteLink {
    @JsonProperty("linkType")
    private String linkType;

    @JsonProperty("targetNoteId")
    private Integer targetNoteId;
  }

  @Data
  public static class MemoryStatus {
    @JsonProperty("lastRecalledAt")
    private LocalDateTime lastRecalledAt;

    @JsonProperty("nextRecallAt")
    private LocalDateTime nextRecallAt;

    @JsonProperty("repetitionCount")
    private Integer repetitionCount;

    @JsonProperty("forgettingCurveIndex")
    private Integer forgettingCurveIndex;
  }
}

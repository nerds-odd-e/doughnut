package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipLiteralSearchHit {

  public enum HitKind {
    NOTE,
    FOLDER
  }

  @NotNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  private HitKind hitKind;

  @Schema(description = "Present when hitKind is NOTE")
  private NoteSearchResult noteSearchResult;

  @Schema(description = "Present when hitKind is FOLDER")
  private Integer folderId;

  @Schema(description = "Present when hitKind is FOLDER")
  private String folderName;

  @Schema(description = "Notebook for a folder hit")
  private Integer notebookId;

  @Schema(description = "Notebook display name for a folder hit")
  private String notebookName;

  @Schema(description = "Match score for a folder hit (0 exact, 0.9 partial)")
  private Float distance;

  public static RelationshipLiteralSearchHit note(NoteSearchResult noteSearchResult) {
    RelationshipLiteralSearchHit hit = new RelationshipLiteralSearchHit();
    hit.setHitKind(HitKind.NOTE);
    hit.setNoteSearchResult(noteSearchResult);
    return hit;
  }

  public static RelationshipLiteralSearchHit folder(
      Integer folderId,
      String folderName,
      Integer notebookId,
      String notebookName,
      float distance) {
    RelationshipLiteralSearchHit hit = new RelationshipLiteralSearchHit();
    hit.setHitKind(HitKind.FOLDER);
    hit.setFolderId(folderId);
    hit.setFolderName(folderName);
    hit.setNotebookId(notebookId);
    hit.setNotebookName(notebookName);
    hit.setDistance(distance);
    return hit;
  }

  @JsonIgnore
  public boolean isNote() {
    return hitKind == HitKind.NOTE;
  }

  @JsonIgnore
  public boolean isFolder() {
    return hitKind == HitKind.FOLDER;
  }
}

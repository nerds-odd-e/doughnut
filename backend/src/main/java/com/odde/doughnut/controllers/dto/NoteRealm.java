package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.entities.Note;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

@JsonPropertyOrder({
  "id",
  "note",
  "fromBazaar",
  "notebookId",
  "ancestorFolders",
  "relationshipsDeprecating",
  "inboundReferences",
  "wikiTitles"
})
public class NoteRealm {
  @Getter @Setter private List<Note> inboundReferences;

  @NotNull @Getter private Note note;

  @Getter @Setter private Boolean fromBazaar;

  @Getter @Setter private List<FolderTrailSegment> ancestorFolders = List.of();

  @Getter private final List<WikiTitle> wikiTitles;

  public NoteRealm(Note note, List<WikiTitle> wikiTitles) {
    this.note = note;
    this.wikiTitles = List.copyOf(wikiTitles);
  }

  @NotNull
  public Integer getId() {
    return note.getId();
  }

  @JsonProperty("relationshipsDeprecating")
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public List<Note> getRelationshipsDeprecating() {
    return note.getRelationships();
  }

  @NonNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public Integer getNotebookId() {
    return Objects.requireNonNull(note.getNotebook()).getId();
  }
}

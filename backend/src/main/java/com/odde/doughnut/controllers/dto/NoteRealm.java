package com.odde.doughnut.controllers.dto;

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
  "inboundReferences",
  "references",
  "wikiTitles"
})
public class NoteRealm {
  @Getter @Setter private List<Note> inboundReferences;

  /**
   * Referring notes merged from inbound wiki-link rows and subject/parent-linked rows: same
   * authorization as those slices, deduplicated by referring note id, sorted by note id ascending.
   */
  @Getter @Setter private List<Note> references;

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

  @NonNull
  @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
  public Integer getNotebookId() {
    return Objects.requireNonNull(note.getNotebook()).getId();
  }
}

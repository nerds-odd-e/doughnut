package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Note;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@JsonPropertyOrder({
  "id",
  "note",
  "notebookView",
  "ancestorFolders",
  "references",
  "wikiTitles",
  "indexNote"
})
public class NoteRealm {
  /**
   * Referring notes (wiki-title cache inbound links), as {@link NoteTopology}, deduplicated by
   * referring note id and ordered by id, with the same visibility rules as {@link
   * com.odde.doughnut.services.WikiTitleCacheService#referencesNotesForViewer}.
   */
  @Getter @Setter private List<NoteTopology> references;

  @NotNull @Getter private Note note;

  @NotNull @Getter @Setter private NotebookClientView notebookView;

  @Getter @Setter private List<Folder> ancestorFolders = List.of();

  /** True when this note is the designated index note for its containing notebook or folder. */
  @Getter @Setter private boolean indexNote;

  @Getter private final List<WikiTitle> wikiTitles;

  public NoteRealm(Note note, List<WikiTitle> wikiTitles) {
    this.note = note;
    this.wikiTitles = List.copyOf(wikiTitles);
  }

  @NotNull
  public Integer getId() {
    return note.getId();
  }
}

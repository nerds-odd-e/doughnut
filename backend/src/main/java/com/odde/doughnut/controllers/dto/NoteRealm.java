package com.odde.doughnut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
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
  "indexNoteContent"
})
public class NoteRealm {
  /**
   * Referring notes (wiki-title cache inbound links), as {@link NoteTopology}, deduplicated by
   * referring note id and ordered by id, with the same visibility rules as {@link
   * com.odde.doughnut.services.WikiTitleCacheService#referencesNotesForViewer}.
   */
  @Getter @Setter private List<NoteTopology> references;

  @NotNull @Getter private Note note;

  @JsonUnwrapped private final RealmNotebookSidebar sidebar = new RealmNotebookSidebar();

  @Getter private final List<WikiTitle> wikiTitles;

  public NoteRealm(Note note, List<WikiTitle> wikiTitles) {
    this.note = note;
    this.wikiTitles = List.copyOf(wikiTitles);
  }

  @NotNull
  public Integer getId() {
    return note.getId();
  }

  public NotebookClientView getNotebookView() {
    return sidebar.getNotebookView();
  }

  public void setNotebookView(NotebookClientView notebookView) {
    sidebar.setNotebookView(notebookView);
  }

  public List<Folder> getAncestorFolders() {
    return sidebar.getAncestorFolders();
  }

  public void setAncestorFolders(List<Folder> ancestorFolders) {
    sidebar.setAncestorFolders(ancestorFolders);
  }

  public String getIndexNoteContent() {
    return sidebar.getIndexNoteContent();
  }

  public void setIndexNoteContent(String indexNoteContent) {
    sidebar.setIndexNoteContent(indexNoteContent);
  }
}

package com.odde.donut.controllers.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@JsonPropertyOrder({
  "id",
  "note",
  "notebookRealm",
  "ancestorFolders",
  "references",
  "wikiLinks",
  "scopedReadmeContent"
})
public class NoteRealm {
  /**
   * Referring notes (live-resolved authored note references), as {@link NoteTopology}, deduplicated
   * by referring note id and ordered by id, per {@link
   * com.odde.donut.entities.repositories.AuthoredNoteReferenceInboundFacade#distinctReferrerNotesForViewer}.
   */
  @Getter @Setter private List<NoteTopology> references;

  @NotNull @Getter private Note note;

  @JsonUnwrapped private final RealmNotebookSidebar sidebar = new RealmNotebookSidebar();

  @Getter private final List<WikiLink> wikiLinks;

  public NoteRealm(Note note, List<WikiLink> wikiLinks) {
    this.note = note;
    this.wikiLinks = List.copyOf(wikiLinks);
  }

  @NotNull
  public Integer getId() {
    return note.getId();
  }

  public NotebookRealm getNotebookRealm() {
    return sidebar.getNotebookRealm();
  }

  public void setNotebookRealm(NotebookRealm notebookRealm) {
    sidebar.setNotebookRealm(notebookRealm);
  }

  public List<Folder> getAncestorFolders() {
    return sidebar.getAncestorFolders();
  }

  public void setAncestorFolders(List<Folder> ancestorFolders) {
    sidebar.setAncestorFolders(ancestorFolders);
  }

  public String getScopedReadmeContent() {
    return sidebar.getScopedReadmeContent();
  }

  public void setScopedReadmeContent(String scopedReadmeContent) {
    sidebar.setScopedReadmeContent(scopedReadmeContent);
  }
}

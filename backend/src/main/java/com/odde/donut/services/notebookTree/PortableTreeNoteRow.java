package com.odde.donut.services.notebookTree;

import com.odde.donut.entities.DisplayName;

public record PortableTreeNoteRow(Integer folderId, String title, String content) {
  public PortableTreeNoteRow(Integer folderId, DisplayName title, String content) {
    this(folderId, title.value(), content);
  }
}

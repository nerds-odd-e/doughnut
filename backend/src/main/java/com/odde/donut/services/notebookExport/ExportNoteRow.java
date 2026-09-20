package com.odde.donut.services.notebookExport;

import com.odde.donut.entities.DisplayName;

public record ExportNoteRow(Integer folderId, String title, String content) {
  public ExportNoteRow(Integer folderId, DisplayName title, String content) {
    this(folderId, title.value(), content);
  }
}

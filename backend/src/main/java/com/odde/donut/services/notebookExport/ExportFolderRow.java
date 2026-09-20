package com.odde.donut.services.notebookExport;

import com.odde.donut.entities.DisplayName;

public record ExportFolderRow(
    Integer id, Integer parentFolderId, String name, String readmeContent) {
  public ExportFolderRow(
      Integer id, Integer parentFolderId, DisplayName name, String readmeContent) {
    this(id, parentFolderId, name.value(), readmeContent);
  }
}

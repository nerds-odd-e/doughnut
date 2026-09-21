package com.odde.donut.services.notebookTree;

import com.odde.donut.entities.DisplayName;

public record PortableTreeFolderRow(
    Integer id, Integer parentFolderId, String name, String readmeContent) {
  public PortableTreeFolderRow(
      Integer id, Integer parentFolderId, DisplayName name, String readmeContent) {
    this(id, parentFolderId, name.value(), readmeContent);
  }
}

package com.odde.doughnut.entities.repositories;

/** Closed projection for {@link FolderRepository#findIndexRowsByNotebookIdOrderByIdAsc}. */
public interface FolderIndexRowProjection {
  Integer getId();

  String getName();

  Integer getParentFolderId();
}

package com.odde.doughnut.services.index;

import com.odde.doughnut.entities.Folder;
import com.odde.doughnut.entities.Notebook;

/** Structural scope for a notebook or folder index note (designated landing note). */
public sealed interface IndexScope permits IndexScope.NotebookRoot, IndexScope.FolderIndex {

  record NotebookRoot(Notebook notebook) implements IndexScope {}

  record FolderIndex(Folder folder) implements IndexScope {}
}

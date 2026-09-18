package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;

/** Computes the Portable path of live content by walking its folder ancestry. */
final class NotebookGitLivePortablePath {

  private NotebookGitLivePortablePath() {}

  static String ofNote(Note note) {
    return folderPath(note.getFolder()) + note.getTitle() + ".md";
  }

  /** Notebook root ({@code folder == null}) has an empty prefix. */
  static String folderPath(Folder folder) {
    return folder == null ? "" : folderPath(folder.getParentFolder()) + folder.getName() + "/";
  }
}

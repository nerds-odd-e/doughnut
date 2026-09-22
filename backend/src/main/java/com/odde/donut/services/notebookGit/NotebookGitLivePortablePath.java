package com.odde.donut.services.notebookGit;

import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;

/** Computes the Portable path of live content by walking its folder ancestry. */
final class NotebookGitLivePortablePath {

  private NotebookGitLivePortablePath() {}

  static String ofNote(Note note) {
    return ofNote(note.getFolder(), note.getTitle());
  }

  static String ofNote(Folder folder, String title) {
    return ofNote(folderPath(folder), title);
  }

  static String ofNote(String folderPrefix, String title) {
    return folderPrefix + title + ".md";
  }

  /** Notebook root ({@code folder == null}) has an empty prefix. */
  static String folderPath(Folder folder) {
    return folder == null ? "" : folderPath(folder.getParentFolder()) + folder.getName() + "/";
  }

  /** The prefix of the directory holding a file path; empty at the notebook root. */
  static String directoryOf(String path) {
    return path.substring(0, path.lastIndexOf('/') + 1);
  }

  /** The prefix of the directory holding a folder prefix; empty for a root folder. */
  static String parentOf(String prefix) {
    return directoryOf(prefix.substring(0, prefix.length() - 1));
  }
}

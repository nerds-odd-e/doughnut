package com.odde.donut.services.notebookGit;

import java.util.Collection;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;

/** What a Portable-tree path is; the one owner of path classification for notebook trees. */
public enum PortablePathKind {
  MARKDOWN,
  /**
   * A {@code .keep} below the root marks an empty Folder; it carries no note or README identity.
   */
  EMPTY_FOLDER_MARKER,
  /** Anything else that is not reserved Git metadata, at whatever depth. */
  ATTACHMENT,
  METADATA;

  private static final ObjectId EMPTY_BLOB =
      new ObjectInserter.Formatter().idFor(Constants.OBJ_BLOB, new byte[0]);

  public static PortablePathKind of(String path) {
    if (path.endsWith(".md")) {
      return MARKDOWN;
    }
    if (path.endsWith("/.keep")) {
      return EMPTY_FOLDER_MARKER;
    }
    if (NotebookGitAttributes.isMetadataPath(path)) {
      return METADATA;
    }
    return ATTACHMENT;
  }

  /** A marker holds its folder empty only while it is itself empty and alone in that folder. */
  static boolean isLeftoverFolderMarker(
      String path, ObjectId blobId, Collection<String> treePaths) {
    if (of(path) != EMPTY_FOLDER_MARKER) {
      return false;
    }
    String folder = path.substring(0, path.length() - ".keep".length());
    return !EMPTY_BLOB.equals(blobId)
        || treePaths.stream().anyMatch(other -> !other.equals(path) && other.startsWith(folder));
  }

  /** Markdown notes, READMEs and Attachments carry content; markers and metadata do not. */
  boolean carriesPortableContent() {
    return this == MARKDOWN || this == ATTACHMENT;
  }
}

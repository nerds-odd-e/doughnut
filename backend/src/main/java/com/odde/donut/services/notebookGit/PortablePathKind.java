package com.odde.donut.services.notebookGit;

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

  /** Markdown notes, READMEs and Attachments carry content; markers and metadata do not. */
  boolean carriesPortableContent() {
    return this == MARKDOWN || this == ATTACHMENT;
  }
}

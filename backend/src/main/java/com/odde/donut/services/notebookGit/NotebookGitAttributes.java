package com.odde.donut.services.notebookGit;

import com.odde.donut.services.notebookTree.PortableTreeEntry;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Reserved Git metadata for notebook Portable trees. {@code .gitattributes} is not an attachment or
 * learning concept; full and derived trees preserve accepted bytes rather than regenerating them on
 * each save. New LFS-mode initialization supplies attributes that track non-Markdown attachments
 * through LFS while exempting Markdown and structural markers.
 */
public final class NotebookGitAttributes {

  public static final String PATH = ".gitattributes";

  /**
   * Initial attributes for an LFS-mode notebook: track everything through LFS, then exempt Markdown
   * and structural {@code .keep} markers (and the attributes file itself).
   */
  public static final String INITIAL_CONTENT =
      """
      * filter=lfs diff=lfs merge=lfs -text
      *.md !filter !diff !merge text
      .gitattributes !filter !diff !merge text
      .keep !filter !diff !merge text
      **/.keep !filter !diff !merge text
      """;

  private NotebookGitAttributes() {}

  public static boolean isMetadataPath(String path) {
    return PATH.equals(path) || path.endsWith("/" + PATH);
  }

  /** Reserved Git metadata entries among {@code entries} (for example {@code .gitattributes}). */
  public static List<PortableTreeEntry> selectFrom(List<PortableTreeEntry> entries) {
    return entries.stream().filter(entry -> isMetadataPath(entry.path())).toList();
  }

  /** The initial {@code .gitattributes} entry for a newly initialized LFS-mode tree. */
  public static PortableTreeEntry initialEntry() {
    return new PortableTreeEntry(PATH, INITIAL_CONTENT.getBytes(StandardCharsets.UTF_8));
  }

  public static List<PortableTreeEntry> initialMetadata() {
    return List.of(initialEntry());
  }
}

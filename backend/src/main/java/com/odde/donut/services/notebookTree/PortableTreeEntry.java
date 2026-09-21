package com.odde.donut.services.notebookTree;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * One file in the canonical Portable-tree snapshot of a notebook: its final path (relative to the
 * notebook root, folders joined with "/") and its exact final file bytes. Entries compare by path
 * and byte content, so snapshots built from different sources are equal exactly when they describe
 * the same files.
 */
public record PortableTreeEntry(String path, byte[] content) {

  /**
   * The entry for a text file (a Markdown note or readme, or the empty structural {@code .keep}),
   * holding the UTF-8 bytes the Portable tree stores. Text becomes file bytes only here.
   */
  public static PortableTreeEntry ofText(String path, String text) {
    return new PortableTreeEntry(path, text.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof PortableTreeEntry entry
        && path.equals(entry.path)
        && Arrays.equals(content, entry.content);
  }

  @Override
  public int hashCode() {
    return 31 * path.hashCode() + Arrays.hashCode(content);
  }

  @Override
  public String toString() {
    return "PortableTreeEntry[path=" + path + ", content=" + content.length + " bytes]";
  }
}

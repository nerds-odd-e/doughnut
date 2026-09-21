package com.odde.donut.services.notebookTree;

import com.odde.donut.algorithms.NoteLeadingFrontmatter;

/** Assembles container readme Markdown with {@code type: Readme}. */
public final class PortableTreeReadmeMarkdown {
  private static final String README_TYPE = "Readme";

  private PortableTreeReadmeMarkdown() {}

  public static String assemble(String content) {
    return NoteLeadingFrontmatter.ensureTypeKey(content, README_TYPE, README_TYPE);
  }
}

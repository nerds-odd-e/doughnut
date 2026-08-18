package com.odde.doughnut.services.notebookExport;

import com.odde.doughnut.algorithms.NoteLeadingFrontmatter;

/** Assembles exported container readme Markdown with export-only {@code type: Readme}. */
public final class ExportReadmeMarkdown {
  private static final String README_TYPE = "Readme";

  private ExportReadmeMarkdown() {}

  public static String assemble(String content) {
    return NoteLeadingFrontmatter.ensureTypeKey(content, README_TYPE, README_TYPE);
  }
}

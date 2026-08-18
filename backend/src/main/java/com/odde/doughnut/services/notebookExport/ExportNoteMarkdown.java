package com.odde.doughnut.services.notebookExport;

import com.odde.doughnut.algorithms.NoteLeadingFrontmatter;
import java.util.Objects;

/**
 * Assembles exported note Markdown: stored content, with codec-wrap {@code title:} when the
 * filename cannot round-trip the display title.
 */
public final class ExportNoteMarkdown {
  private static final String MD_SUFFIX = ".md";

  private ExportNoteMarkdown() {}

  public static String assemble(ExportNoteRow note, String zipEntryFileName) {
    String rawContent = note.content() == null ? "" : note.content();
    if (Objects.equals(conceptFileName(zipEntryFileName), note.title())) {
      return rawContent;
    }
    return NoteLeadingFrontmatter.ensureTitleKey(rawContent, note.title());
  }

  private static String conceptFileName(String zipEntryFileName) {
    if (zipEntryFileName.endsWith(MD_SUFFIX)) {
      return zipEntryFileName.substring(0, zipEntryFileName.length() - MD_SUFFIX.length());
    }
    return zipEntryFileName;
  }
}

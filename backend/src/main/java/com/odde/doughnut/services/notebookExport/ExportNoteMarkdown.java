package com.odde.doughnut.services.notebookExport;

import com.odde.doughnut.algorithms.NoteLeadingFrontmatter;
import java.util.Objects;

/**
 * Assembles an exported note Markdown file: preserved author frontmatter (if any), codec-wrap
 * {@code title:} when the filename cannot round-trip, title heading, and body.
 */
public final class ExportNoteMarkdown {
  private static final String MD_SUFFIX = ".md";

  private ExportNoteMarkdown() {}

  public static String assemble(ExportNoteRow note, String zipEntryFileName) {
    String rawContent = note.content() == null ? "" : note.content();
    String heading = "# " + note.title() + "\n\n";
    String wrapped =
        Objects.equals(conceptFileName(zipEntryFileName), note.title())
            ? rawContent
            : NoteLeadingFrontmatter.ensureTitleKey(rawContent, note.title());
    return NoteLeadingFrontmatter.splitVerbatim(wrapped)
        .map(split -> split.frontmatterBlock() + "\n\n" + heading + split.body().stripLeading())
        .orElseGet(() -> heading + wrapped.stripLeading());
  }

  private static String conceptFileName(String zipEntryFileName) {
    if (zipEntryFileName.endsWith(MD_SUFFIX)) {
      return zipEntryFileName.substring(0, zipEntryFileName.length() - MD_SUFFIX.length());
    }
    return zipEntryFileName;
  }
}

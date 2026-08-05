package com.odde.doughnut.services.notebookExport;

import com.odde.doughnut.algorithms.NoteLeadingFrontmatter;

/**
 * Assembles an exported note Markdown file: preserved author frontmatter (if any), title heading,
 * and body.
 */
public final class ExportNoteMarkdown {
  private ExportNoteMarkdown() {}

  public static String assemble(ExportNoteRow note) {
    String rawContent = note.content() == null ? "" : note.content();
    String heading = "# " + note.title() + "\n\n";
    return NoteLeadingFrontmatter.splitVerbatim(rawContent)
        .map(split -> split.frontmatterBlock() + "\n\n" + heading + split.body().stripLeading())
        .orElseGet(() -> heading + rawContent.stripLeading());
  }
}

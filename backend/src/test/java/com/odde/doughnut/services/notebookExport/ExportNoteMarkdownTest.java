package com.odde.doughnut.services.notebookExport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

class ExportNoteMarkdownTest {

  @Test
  void preservesAuthorFrontmatterWithoutInjectingIdentity() {
    ExportNoteRow note =
        new ExportNoteRow(7, null, "With Fm", "---\nwikidata_id: Q1\n---\n\nBody text");

    String md = ExportNoteMarkdown.assemble(note, "With Fm.md");

    assertThat(md, equalTo("---\nwikidata_id: Q1\n---\n\n# With Fm\n\nBody text"));
  }

  @Test
  void emitsTitleHeadingAndBodyWhenNoFrontmatter() {
    ExportNoteRow note = new ExportNoteRow(4, null, "Lone", "Body only");

    String md = ExportNoteMarkdown.assemble(note, "Lone.md");

    assertThat(md, equalTo("# Lone\n\nBody only"));
  }

  @Test
  void leavesWikiLinksAndAttachmentPathsUnchanged() {
    ExportNoteRow note =
        new ExportNoteRow(
            1, null, "Source", "See [[Target]] and ![](/attachments/images/9/photo.png)");

    String md = ExportNoteMarkdown.assemble(note, "Source.md");

    assertThat(md, equalTo("# Source\n\nSee [[Target]] and ![](/attachments/images/9/photo.png)"));
  }
}

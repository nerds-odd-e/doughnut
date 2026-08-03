package com.odde.doughnut.services.notebookExport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExportNoteMarkdownTest {

  @Test
  void mergesDoughnutIdReplacingExistingCaseInsensitiveLine() {
    String block = "---\nDOUGHNUT_ID: 99\nwikidata_id: Q1\n---";
    assertThat(
        ExportNoteMarkdown.mergeDoughnutId(block, 7),
        equalTo("---\ndoughnut_id: 7\nwikidata_id: Q1\n---"));
  }

  @Test
  void emitsMinimalIdentityFenceWhenNoFrontmatter() {
    ExportNoteRow note = new ExportNoteRow(4, null, "Lone", "Body only");
    Map<Integer, String> paths = Map.of(4, "Lone.md");
    Map<String, Integer> titles = Map.of("Lone", 4);

    String md =
        ExportNoteMarkdown.assemble(note, "Nb", "http://localhost:9081", "Lone.md", paths, titles);

    assertThat(md, equalTo("---\ndoughnut_id: 4\n---\n\n# Lone\n\nBody only"));
  }

  @Test
  void leavesUnresolvedWikiUnchangedIncludingOtherNotebook() {
    String markdown = "See [[Missing]] and [[Other Nb:Target]]";
    Map<Integer, String> paths = Map.of(1, "Here.md", 2, "Target.md");
    Map<String, Integer> titles = Map.of("Target", 2);

    String result =
        ExportNoteMarkdown.rewriteWikiLinks(markdown, "Here Nb", "Here.md", paths, titles);

    assertThat(result, equalTo("See [[Missing]] and [[Other Nb:Target]]"));
  }

  @Test
  void relativizesNestedSourceToSiblingNote() {
    assertThat(
        ExportNoteMarkdown.relativizeZipPath("Folder/Child/Source.md", "Folder/Sibling.md"),
        equalTo("../Sibling.md"));
  }

  @Test
  void prefixesAttachmentPathsWithPublicOrigin() {
    String md = "![x](/attachments/images/3/a.png)";
    assertThat(
        ExportNoteMarkdown.rewriteAttachmentPaths(md, "http://localhost:9081/"),
        equalTo("![x](http://localhost:9081/attachments/images/3/a.png)"));
  }

  @Test
  void assemblesCollisionSafeWikiTargetPath() {
    ExportNoteRow source = new ExportNoteRow(10, null, "Linker", "Go to [[Dup]]");
    Map<Integer, String> paths = new LinkedHashMap<>();
    paths.put(1, "Dup.md");
    paths.put(2, "Dup (2).md");
    paths.put(10, "Linker.md");
    Map<String, Integer> titles = new LinkedHashMap<>();
    titles.put("Dup", 1);
    titles.put("Linker", 10);

    String md =
        ExportNoteMarkdown.assemble(
            source, "Nb", "http://localhost:9081", "Linker.md", paths, titles);

    assertThat(md, containsString("[Dup](Dup.md)"));
    assertThat(md, not(containsString("[[Dup]]")));
    assertThat(md, not(containsString("Dup%20(2)")));
  }
}

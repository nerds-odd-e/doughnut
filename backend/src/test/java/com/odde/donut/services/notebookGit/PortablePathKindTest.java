package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PortablePathKindTest {

  @ParameterizedTest
  @CsvSource({
    "Note.md, MARKDOWN",
    "Folder/README.md, MARKDOWN",
    "Folder/.keep, EMPTY_FOLDER_MARKER",
    ".keep, ATTACHMENT",
    "diagram.png, ATTACHMENT",
    "Folder/diagram.png, ATTACHMENT",
    ".gitattributes, METADATA",
    "nested/.gitattributes, METADATA",
  })
  void classifiesEachPath(String path, PortablePathKind kind) {
    assertThat(PortablePathKind.of(path), is(kind));
  }

  @ParameterizedTest
  @CsvSource({
    "Note.md, true",
    "diagram.png, true",
    "Folder/.keep, false",
    ".gitattributes, false",
  })
  void onlyMarkdownAndAttachmentsCarryPortableContent(String path, boolean carries) {
    assertThat(PortablePathKind.of(path).carriesPortableContent(), is(carries));
  }
}

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
    "Forces.MD, ATTACHMENT",
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

  @ParameterizedTest
  @CsvSource({
    "Forces.MD, true",
    "Physics/Notes.Md, true",
    "README.mD, true",
    "Note.md, false",
    "diagram.png, false",
    "Folder.MD/diagram.png, false",
  })
  void recognisesAMarkdownExtensionInAnotherLetterCase(String path, boolean miscased) {
    assertThat(PortablePathKind.hasMiscasedMarkdownExtension(path), is(miscased));
  }

  @ParameterizedTest
  @CsvSource({
    "README.md, NOTEBOOK_README",
    "Folder/README.md, FOLDER_README",
    "Folder/Sub/README.md, FOLDER_README",
    "Note.md, NOTE",
    "Folder/readme.md, NOTE",
    "Folder/NOT-README.md, NOTE",
  })
  void tellsTheReadmeRoleOfAMarkdownPath(String path, PortablePathKind.MarkdownRole role) {
    assertThat(PortablePathKind.markdownRole(path), is(role));
  }
}

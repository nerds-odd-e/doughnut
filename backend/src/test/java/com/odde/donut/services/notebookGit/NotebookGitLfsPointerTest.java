package com.odde.donut.services.notebookGit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NotebookGitLfsPointerTest {

  private static final String OID =
      "4d7a214614ab2935c943f9e0ff69d22eadbb8f32b1258daaa5e2ca24d17e2393";

  @Test
  void classifiesCanonicalPointerAndEmptyFile() {
    byte[] pointer = NotebookGitLfsPointer.format(OID, 12345);
    Optional<NotebookGitLfsPointer.Parsed> parsed = NotebookGitLfsPointer.parse(pointer);

    assertThat(parsed.isPresent(), is(true));
    assertThat(parsed.orElseThrow().sha256Hex(), equalTo(OID));
    assertThat(parsed.orElseThrow().size(), equalTo(12345L));
    assertTrue(NotebookGitLfsPointer.isEmptyFile(new byte[0]));
    assertFalse(NotebookGitLfsPointer.isPointer(new byte[0]));
    assertFalse(NotebookGitLfsPointer.isPointer("not a pointer".getBytes(StandardCharsets.UTF_8)));
  }
}

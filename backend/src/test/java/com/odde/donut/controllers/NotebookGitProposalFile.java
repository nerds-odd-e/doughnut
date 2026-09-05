package com.odde.donut.controllers;

import java.nio.charset.StandardCharsets;
import org.eclipse.jgit.lib.FileMode;

/** One file's path, content bytes, and mode within a crafted notebook Git proposal tree. */
record NotebookGitProposalFile(String path, byte[] contentBytes, FileMode mode) {
  NotebookGitProposalFile(String path, String content, FileMode mode) {
    this(path, content.getBytes(StandardCharsets.UTF_8), mode);
  }

  NotebookGitProposalFile(String path, String content) {
    this(path, content, FileMode.REGULAR_FILE);
  }

  /** For deliberately-invalid byte sequences (e.g. malformed UTF-8) that no String can hold. */
  NotebookGitProposalFile(String path, byte[] contentBytes) {
    this(path, contentBytes, FileMode.REGULAR_FILE);
  }
}

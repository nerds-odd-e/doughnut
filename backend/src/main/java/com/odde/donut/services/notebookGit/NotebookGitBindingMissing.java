package com.odde.donut.services.notebookGit;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** The refusal for a Git operation on a notebook that has no Git binding. */
final class NotebookGitBindingMissing {
  private NotebookGitBindingMissing() {}

  static ResponseStatusException refusal() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Notebook has no Git binding.");
  }
}

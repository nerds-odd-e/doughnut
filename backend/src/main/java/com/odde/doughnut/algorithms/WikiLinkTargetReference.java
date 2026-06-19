package com.odde.doughnut.algorithms;

import java.util.Optional;

/** Notebook name and note title used to resolve a wiki-link token to a target note. */
public record WikiLinkTargetReference(String notebookName, String noteTitle) {

  /**
   * Parses a wiki-link inner token into a notebook/title pair. Qualified links use {@code
   * Notebook:Title}; unqualified links use {@code focusNotebookName} as the notebook.
   */
  public static Optional<WikiLinkTargetReference> forToken(String token, String focusNotebookName) {
    String resolutionKey = WikiLinkMarkdown.splitInner(token).target();
    if (resolutionKey == null || resolutionKey.isBlank()) {
      return Optional.empty();
    }
    Qualified qualified = Qualified.tryParse(resolutionKey);
    if (qualified != null) {
      return Optional.of(
          new WikiLinkTargetReference(qualified.notebookName(), qualified.noteTitle()));
    }
    if (focusNotebookName == null || focusNotebookName.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(new WikiLinkTargetReference(focusNotebookName, resolutionKey));
  }

  private record Qualified(String notebookName, String noteTitle) {
    static Qualified tryParse(String token) {
      int i = token.indexOf(':');
      if (i <= 0 || i >= token.length() - 1) {
        return null;
      }
      String nb = token.substring(0, i).trim();
      String nt = token.substring(i + 1).trim();
      if (nb.isEmpty() || nt.isEmpty()) {
        return null;
      }
      return new Qualified(nb, nt);
    }
  }
}

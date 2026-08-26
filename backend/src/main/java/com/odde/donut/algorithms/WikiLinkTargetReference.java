package com.odde.donut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/** Notebook name and note title used to resolve a wiki-link token to a target note. */
public record WikiLinkTargetReference(String notebookName, String noteTitle) {

  /**
   * Parses a wiki-link inner token into a notebook/title pair. Qualified links use {@code
   * Notebook:Title}; unqualified links use {@code focusNotebookName} as the notebook.
   */
  public static Optional<WikiLinkTargetReference> forToken(String token, String focusNotebookName) {
    String resolutionKey = WikiLinkMarkdown.splitAuthoredToken(token).target();
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

  static boolean isQualifiedToken(String targetToken) {
    return Qualified.tryParse(targetToken) != null;
  }

  static String replaceNoteTitle(String targetToken, String newTitle) {
    Qualified qualified = Qualified.tryParse(targetToken);
    if (qualified != null) {
      return qualified.notebookName() + ":" + newTitle;
    }
    return PathShapedTarget.tryParse(targetToken)
        .map(path -> path.withNoteTitle(newTitle))
        .orElse(newTitle);
  }

  static String replaceFolderName(String targetToken, String oldFolderName, String newFolderName) {
    return PathShapedTarget.tryParse(targetToken)
        .map(path -> path.withRenamedFolder(oldFolderName, newFolderName))
        .orElse(targetToken);
  }

  static String replaceNotebookName(String targetToken, String newNotebookName) {
    Qualified qualified = Qualified.tryParse(targetToken);
    String noteTitle = qualified == null ? targetToken : qualified.noteTitle();
    return newNotebookName + ":" + noteTitle;
  }

  /**
   * Folder names then title for a path-shaped target ({@code Folder/Title}, or Markdown {@code
   * /Title} at notebook root). Resolution ignores optional trailing {@code .md}; {@link
   * #withNoteTitle} keeps leading {@code /} and that suffix. Unqualified titles without a leading
   * {@code /} are not path-shaped.
   */
  public record PathShapedTarget(
      List<String> folderNames, String title, boolean leadingSlash, boolean markdownSuffix) {
    public static Optional<PathShapedTarget> tryParse(String noteTitle) {
      if (noteTitle == null || noteTitle.contains(":")) {
        return Optional.empty();
      }
      boolean leadingSlash = noteTitle.startsWith("/");
      if (!leadingSlash && !noteTitle.contains("/")) {
        return Optional.empty();
      }
      List<String> segments = new ArrayList<>();
      for (String raw : noteTitle.split("/", -1)) {
        String segment = raw.trim();
        if (!segment.isEmpty()) {
          segments.add(segment);
        }
      }
      if (segments.isEmpty()) {
        return Optional.empty();
      }
      if (segments.size() < 2 && !leadingSlash) {
        return Optional.empty();
      }
      String last = segments.getLast();
      boolean markdownSuffix = hasOptionalMarkdownSuffix(last);
      String title = markdownSuffix ? last.substring(0, last.length() - 3) : last;
      if (title.isEmpty()) {
        return Optional.empty();
      }
      List<String> folderNames =
          segments.size() == 1 ? List.of() : List.copyOf(segments.subList(0, segments.size() - 1));
      return Optional.of(new PathShapedTarget(folderNames, title, leadingSlash, markdownSuffix));
    }

    String withNoteTitle(String newTitle) {
      return formatPath(folderNames, newTitle);
    }

    public String mapSegmentNames(UnaryOperator<String> map) {
      List<String> mapped = new ArrayList<>(folderNames.size());
      for (String folderName : folderNames) {
        mapped.add(map.apply(folderName));
      }
      return formatPath(mapped, map.apply(title));
    }

    String withRenamedFolder(String oldFolderName, String newFolderName) {
      List<String> updated = new ArrayList<>(folderNames);
      for (int i = 0; i < updated.size(); i++) {
        if (updated.get(i).equalsIgnoreCase(oldFolderName)) {
          updated.set(i, newFolderName);
          break;
        }
      }
      return formatPath(updated, title);
    }

    public boolean matchesTitleAndFolderTrail(String noteTitle, List<String> folderTrailNames) {
      if (!noteTitle.equalsIgnoreCase(title)) {
        return false;
      }
      if (folderTrailNames.size() != folderNames.size()) {
        return false;
      }
      for (int i = 0; i < folderNames.size(); i++) {
        if (!folderNames.get(i).equalsIgnoreCase(folderTrailNames.get(i))) {
          return false;
        }
      }
      return true;
    }

    private String formatPath(List<String> folders, String noteTitle) {
      StringBuilder out = new StringBuilder();
      if (leadingSlash) {
        out.append('/');
      }
      if (!folders.isEmpty()) {
        out.append(String.join("/", folders)).append('/');
      }
      out.append(noteTitle);
      if (markdownSuffix) {
        out.append(".md");
      }
      return out.toString();
    }

    private static boolean hasOptionalMarkdownSuffix(String lastSegment) {
      return lastSegment.length() > 3
          && lastSegment.regionMatches(true, lastSegment.length() - 3, ".md", 0, 3);
    }
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

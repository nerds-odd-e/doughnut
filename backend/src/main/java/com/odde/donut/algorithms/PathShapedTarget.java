package com.odde.donut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Folder names then title for a path-shaped note portion ({@code Folder/Title}, or Markdown {@code
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
    for (String segmentRaw : noteTitle.split("/", -1)) {
      String segment = segmentRaw.trim();
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

  /**
   * Formats this title (and optional {@code .md}) at a new folder trail. Empty trail uses
   * exact-root {@code /Title} spelling.
   */
  String atFolderTrail(List<String> newFolderNames) {
    List<String> folders = newFolderNames == null ? List.of() : List.copyOf(newFolderNames);
    return formatPath(folders, title, folders.isEmpty());
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
    return formatPath(folders, noteTitle, leadingSlash);
  }

  private String formatPath(List<String> folders, String noteTitle, boolean useLeadingSlash) {
    StringBuilder out = new StringBuilder();
    if (useLeadingSlash) {
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

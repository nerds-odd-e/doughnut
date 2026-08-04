package com.odde.doughnut.services.notebookExport;

import com.odde.doughnut.algorithms.NoteLeadingFrontmatter;
import com.odde.doughnut.algorithms.WikiLinkMarkdown;
import com.odde.doughnut.algorithms.WikiLinkTargetReference;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Assembles an exported note Markdown file: preserved author frontmatter (if any), title heading,
 * body, then wiki→relative MD and attachment absolute-URL rewrites.
 */
public final class ExportNoteMarkdown {
  private static final Pattern ATTACHMENT_IMAGE_PATH =
      Pattern.compile("(?<![\\w:])/attachments/images/(\\d+)/([^\\s)\\]\"']*)");

  private ExportNoteMarkdown() {}

  public static String assemble(
      ExportNoteRow note,
      String notebookName,
      String publicOrigin,
      String zipRelativePath,
      Map<Integer, String> noteIdToZipPath,
      Map<String, Integer> titleToNoteId) {
    String rawContent = note.content() == null ? "" : note.content();
    String heading = "# " + note.title() + "\n\n";
    String withHeading =
        NoteLeadingFrontmatter.splitVerbatim(rawContent)
            .map(split -> split.frontmatterBlock() + "\n\n" + heading + split.body().stripLeading())
            .orElseGet(() -> heading + rawContent.stripLeading());
    String withWiki =
        rewriteWikiLinks(
            withHeading, notebookName, zipRelativePath, noteIdToZipPath, titleToNoteId);
    return rewriteAttachmentPaths(withWiki, publicOrigin);
  }

  static String rewriteWikiLinks(
      String markdown,
      String notebookName,
      String sourceZipPath,
      Map<Integer, String> noteIdToZipPath,
      Map<String, Integer> titleToNoteId) {
    if (markdown == null || markdown.isEmpty()) {
      return markdown;
    }
    Matcher matcher = WikiLinkMarkdown.INNER_LINK_PATTERN.matcher(markdown);
    StringBuilder out = new StringBuilder();
    int last = 0;
    while (matcher.find()) {
      out.append(markdown, last, matcher.start());
      String rawInner = matcher.group(1);
      String replacement =
          resolveWikiToMarkdownLink(
                  rawInner, notebookName, sourceZipPath, noteIdToZipPath, titleToNoteId)
              .orElse(matcher.group(0));
      out.append(replacement);
      last = matcher.end();
    }
    out.append(markdown.substring(last));
    return out.toString();
  }

  private static Optional<String> resolveWikiToMarkdownLink(
      String rawInner,
      String notebookName,
      String sourceZipPath,
      Map<Integer, String> noteIdToZipPath,
      Map<String, Integer> titleToNoteId) {
    return WikiLinkTargetReference.forToken(rawInner, notebookName)
        .filter(ref -> notebookName != null && notebookName.equals(ref.notebookName()))
        .flatMap(
            ref -> {
              Integer targetId = titleToNoteId.get(ref.noteTitle());
              if (targetId == null) {
                return Optional.empty();
              }
              String targetPath = noteIdToZipPath.get(targetId);
              if (targetPath == null) {
                return Optional.empty();
              }
              String display = WikiLinkMarkdown.splitInner(rawInner).display();
              String relative = relativizeZipPath(sourceZipPath, targetPath);
              return Optional.of("[" + display + "](" + encodeRelativePath(relative) + ")");
            });
  }

  static String relativizeZipPath(String sourceZipPath, String targetZipPath) {
    Path sourceParent = Path.of(sourceZipPath).getParent();
    if (sourceParent == null) {
      sourceParent = Path.of("");
    }
    return sourceParent.relativize(Path.of(targetZipPath)).toString().replace('\\', '/');
  }

  static String encodeRelativePath(String path) {
    return Arrays.stream(path.split("/", -1))
        .map(seg -> seg.replace(" ", "%20"))
        .collect(Collectors.joining("/"));
  }

  static String rewriteAttachmentPaths(String markdown, String publicOrigin) {
    if (markdown == null || markdown.isEmpty() || publicOrigin == null || publicOrigin.isBlank()) {
      return markdown;
    }
    String origin =
        publicOrigin.endsWith("/")
            ? publicOrigin.substring(0, publicOrigin.length() - 1)
            : publicOrigin;
    Matcher matcher = ATTACHMENT_IMAGE_PATH.matcher(markdown);
    StringBuilder out = new StringBuilder();
    while (matcher.find()) {
      matcher.appendReplacement(out, Matcher.quoteReplacement(origin + matcher.group(0)));
    }
    matcher.appendTail(out);
    return out.toString();
  }
}

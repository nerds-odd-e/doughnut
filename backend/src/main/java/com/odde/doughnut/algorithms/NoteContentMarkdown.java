package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.util.Strings;

/**
 * Note content helpers for the leading YAML block: orchestrates fence parsing ({@link
 * NoteLeadingFrontmatter}) and generic YAML line handling ({@link YamlHandler}).
 */
public final class NoteContentMarkdown {

  private NoteContentMarkdown() {}

  private static final String WIKIDATA_ID_KEY = "wikidata_id";
  private static final String NOTE_IMAGE_KEY = "image";
  private static final String NOTE_IMAGE_MASK_KEY = "image_mask";

  private static final Set<String> NOTE_IMAGE_MAPPING_KEYS =
      Set.of(NOTE_IMAGE_KEY, NOTE_IMAGE_MASK_KEY);

  public record LeadingFrontmatter(String yamlRaw, String body) {}

  public static String bodyWithoutLeadingFrontmatter(String content) {
    return splitLeadingFrontmatter(content).map(LeadingFrontmatter::body).orElse(content);
  }

  /**
   * Reads a Wikidata Q-id from the first YAML frontmatter block when present, using {@code
   * wikidata_id} (rich-mode property rows).
   */
  public static Optional<String> wikidataIdScalarFromLeadingFrontmatter(String content) {
    return splitLeadingFrontmatter(content)
        .flatMap(lf -> YamlHandler.firstScalarValue(lf.yamlRaw(), WIKIDATA_ID_KEY));
  }

  public static Optional<LeadingFrontmatter> splitLeadingFrontmatter(String content) {
    return NoteLeadingFrontmatter.split(content)
        .map(s -> new LeadingFrontmatter(s.yamlRaw(), s.body()));
  }

  /**
   * Updates or removes {@code image:} and {@code image_mask:} scalar lines in the first leading
   * YAML frontmatter block so note display can read a single source of truth from {@code
   * note.content}. When {@code hasImage} is false, both lines are removed if present.
   */
  public static String mergeNoteImageScalarsIntoContent(
      String content, boolean hasImage, String imageUrl, String imageMask) {
    if (content == null) {
      content = "";
    }
    Optional<LeadingFrontmatter> split = splitLeadingFrontmatter(content);
    if (split.isEmpty()) {
      if (!hasImage) {
        return content;
      }
      return NoteLeadingFrontmatter.contentWithLeadingFence(
          prependNoteImageScalarLines(imageUrl, imageMask), content);
    }
    LeadingFrontmatter lf = split.get();
    List<String> kept =
        YamlHandler.linesOmittingMappingKeys(lf.yamlRaw().split("\n", -1), NOTE_IMAGE_MAPPING_KEYS);
    if (hasImage && !Strings.isBlank(imageUrl)) {
      kept.add(YamlHandler.mappingLine(NOTE_IMAGE_KEY, imageUrl.trim()));
      if (!Strings.isBlank(imageMask)) {
        kept.add(YamlHandler.mappingLine(NOTE_IMAGE_MASK_KEY, imageMask.trim()));
      }
    }
    return NoteLeadingFrontmatter.contentWithLeadingFence(kept, lf.body());
  }

  /** New leading block: always emit {@code image} (matches legacy prepend behavior). */
  private static List<String> prependNoteImageScalarLines(String imageUrl, String imageMask) {
    List<String> lines = new ArrayList<>();
    lines.add(YamlHandler.mappingLine(NOTE_IMAGE_KEY, imageUrl == null ? "" : imageUrl.trim()));
    if (!Strings.isBlank(imageMask)) {
      lines.add(YamlHandler.mappingLine(NOTE_IMAGE_MASK_KEY, imageMask.trim()));
    }
    return lines;
  }

  public static Optional<String> removeWikiLinksFromLeadingFrontmatterProperties(
      String content, Set<String> linkTexts) {
    if (linkTexts.isEmpty()) {
      return Optional.empty();
    }
    Optional<LeadingFrontmatter> split = splitLeadingFrontmatter(content);
    if (split.isEmpty()) {
      return Optional.empty();
    }

    List<String> keptLines = new ArrayList<>();
    boolean changed = false;
    for (String line : split.get().yamlRaw().split("\n", -1)) {
      String rewritten = line;
      for (String linkText : linkTexts) {
        rewritten = rewritten.replace("[[" + linkText + "]]", "");
      }
      if (!rewritten.equals(line)) {
        changed = true;
        if (YamlHandler.isEmptyMappingValueLine(rewritten)) {
          continue;
        }
      }
      keptLines.add(rewritten);
    }
    if (!changed) {
      return Optional.empty();
    }
    String body = split.get().body();
    return Optional.of(NoteLeadingFrontmatter.contentWithLeadingFence(keptLines, body));
  }
}

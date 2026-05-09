package com.odde.doughnut.algorithms;

import java.util.Optional;
import java.util.Set;

/**
 * Note content helpers for the leading YAML block: orchestrates fence parsing ({@link
 * NoteLeadingFrontmatter}) and frontmatter manipulation ({@link Frontmatter}).
 */
public final class NoteContentMarkdown {

  private NoteContentMarkdown() {}

  private static final String WIKIDATA_ID_KEY = "wikidata_id";
  private static final String NOTE_IMAGE_KEY = "image";
  private static final String NOTE_IMAGE_MASK_KEY = "image_mask";

  private static final Set<String> NOTE_IMAGE_MAPPING_KEYS =
      Set.of(NOTE_IMAGE_KEY, NOTE_IMAGE_MASK_KEY);

  public static final class LeadingFrontmatter {
    private final Frontmatter frontmatter;
    private final String body;

    private LeadingFrontmatter(Frontmatter frontmatter, String body) {
      this.frontmatter = frontmatter;
      this.body = body;
    }

    public Frontmatter frontmatter() {
      return frontmatter;
    }

    public String body() {
      return body;
    }
  }

  public static String bodyWithoutLeadingFrontmatter(String content) {
    return splitLeadingFrontmatter(content).map(LeadingFrontmatter::body).orElse(content);
  }

  /**
   * Reads a Wikidata Q-id from the first YAML frontmatter block when present, using {@code
   * wikidata_id} (rich-mode property rows).
   */
  public static Optional<String> wikidataIdScalarFromLeadingFrontmatter(String content) {
    return splitLeadingFrontmatter(content)
        .flatMap(lf -> lf.frontmatter().getString(WIKIDATA_ID_KEY));
  }

  public static Optional<LeadingFrontmatter> splitLeadingFrontmatter(String content) {
    return NoteLeadingFrontmatter.split(content)
        .map(s -> new LeadingFrontmatter(s.frontmatter(), s.body()));
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
      Frontmatter fm = Frontmatter.empty().set(NOTE_IMAGE_KEY, trimOrNull(imageUrl));
      if (imageMask != null && !imageMask.isBlank()) {
        fm = fm.set(NOTE_IMAGE_MASK_KEY, imageMask.trim());
      }
      return fm.fenced(content);
    }
    LeadingFrontmatter lf = split.get();
    Frontmatter fm = lf.frontmatter().remove(NOTE_IMAGE_MAPPING_KEYS);
    if (hasImage && imageUrl != null && !imageUrl.isBlank()) {
      fm = fm.set(NOTE_IMAGE_KEY, imageUrl.trim());
      if (imageMask != null && !imageMask.isBlank()) {
        fm = fm.set(NOTE_IMAGE_MASK_KEY, imageMask.trim());
      }
    }
    return fm.fenced(lf.body());
  }

  public static Optional<String> removeWikiLinksFromLeadingFrontmatterProperties(
      String content, Set<String> linkTexts) {
    if (linkTexts.isEmpty()) {
      return Optional.empty();
    }
    return splitLeadingFrontmatter(content)
        .flatMap(
            lf ->
                lf.frontmatter()
                    .mapStringValues(
                        v -> linkTexts.stream().reduce(v, (s, t) -> s.replace("[[" + t + "]]", "")))
                    .map(updated -> updated.isEmpty() ? lf.body() : updated.fenced(lf.body())));
  }

  private static String trimOrNull(String s) {
    return s == null ? null : s.trim();
  }
}

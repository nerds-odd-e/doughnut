package com.odde.donut.algorithms;

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

  public static boolean isBodyContentBlank(String content) {
    String body = bodyWithoutLeadingFrontmatter(content);
    if (body == null || body.isBlank()) {
      return true;
    }
    return new HtmlOrMarkdown(body).isBlank();
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

  /** Sets the leading frontmatter's {@code image:} scalar, leaving every other property as is. */
  public static String withNoteImage(String content, String image) {
    return setLeadingFrontmatterProperty(content, NOTE_IMAGE_KEY, image);
  }

  /** Sets the leading frontmatter's {@code image_mask:} scalar, leaving other properties as is. */
  public static String withNoteImageMask(String content, String imageMask) {
    return setLeadingFrontmatterProperty(content, NOTE_IMAGE_MASK_KEY, imageMask);
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

  public record AddPropertyWithAvailableKeyResult(String content, String resolvedKey) {}

  /**
   * Appends a scalar property using the next free key in {@code basePropertyKey}'s family (`key 2`,
   * `key 3`, … when the base is taken).
   */
  public static AddPropertyWithAvailableKeyResult addPropertyWithAvailableKeyToLeadingFrontmatter(
      String content, String basePropertyKey, String value) {
    Set<String> existingKeys =
        splitLeadingFrontmatter(content == null ? "" : content)
            .map(lf -> lf.frontmatter().keys())
            .orElse(Set.of());
    String resolvedKey =
        PropertyKeyNaming.nextAvailablePropertyKeyForBase(basePropertyKey, existingKeys);
    return new AddPropertyWithAvailableKeyResult(
        setLeadingFrontmatterProperty(content, resolvedKey, value), resolvedKey);
  }

  /**
   * Sets a scalar property in the first leading YAML frontmatter block, replacing an existing
   * {@code key} (case-insensitive) in place or appending it, and creates the block when absent.
   */
  public static String setLeadingFrontmatterProperty(String content, String key, String value) {
    if (content == null) {
      content = "";
    }
    Optional<LeadingFrontmatter> split = splitLeadingFrontmatter(content);
    if (split.isPresent()) {
      LeadingFrontmatter lf = split.get();
      return lf.frontmatter().set(key, value).fenced(lf.body());
    }
    return Frontmatter.empty().set(key, value).fenced(content);
  }
}

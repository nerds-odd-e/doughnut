package com.odde.doughnut.algorithms;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

  private static final Pattern ATTACHMENT_IMAGE_PATH_PREFIX =
      Pattern.compile("^/attachments/images/(\\d+)/");

  /**
   * How the leading {@code image:} scalar relates to persisted attachment paths for orphan {@link
   * com.odde.doughnut.entities.Image} cleanup.
   */
  public sealed interface LeadingFrontmatterImageReference
      permits LeadingFrontmatterImageReference.None,
          LeadingFrontmatterImageReference.Referenced,
          LeadingFrontmatterImageReference.InvalidPathPresent {

    /** No {@code image:} line, blank value, or no leading frontmatter — delete all note images. */
    record None() implements LeadingFrontmatterImageReference {}

    /**
     * Frontmatter references this attachment image id — keep this row, delete other note images.
     */
    record Referenced(int imageId) implements LeadingFrontmatterImageReference {}

    /**
     * Non-blank {@code image:} value that is not a canonical attachment path — skip cleanup (avoid
     * deleting blobs on typos).
     */
    record InvalidPathPresent() implements LeadingFrontmatterImageReference {}
  }

  /**
   * Parses the first leading YAML block's {@code image:} scalar for attachment id resolution used
   * when deleting orphaned {@link com.odde.doughnut.entities.Image} rows.
   */
  public static LeadingFrontmatterImageReference leadingFrontmatterImageReference(String content) {
    Optional<LeadingFrontmatter> split = splitLeadingFrontmatter(content == null ? "" : content);
    if (split.isEmpty()) {
      return new LeadingFrontmatterImageReference.None();
    }
    Frontmatter fm = split.get().frontmatter();
    if (!fm.containsKeyIgnoreCase(NOTE_IMAGE_KEY)) {
      return new LeadingFrontmatterImageReference.None();
    }
    Optional<String> raw = fm.getString(NOTE_IMAGE_KEY);
    String trimmed = raw.map(String::trim).orElse("");
    if (trimmed.isEmpty()) {
      return new LeadingFrontmatterImageReference.None();
    }
    String normalized = stripSurroundingQuotes(trimmed);
    Optional<Integer> id = attachmentImageIdFromPath(normalized);
    if (id.isPresent()) {
      return new LeadingFrontmatterImageReference.Referenced(id.get());
    }
    return new LeadingFrontmatterImageReference.InvalidPathPresent();
  }

  /** Extracts the numeric image id from a canonical {@code /attachments/images/{id}/…} path. */
  public static Optional<Integer> attachmentImageIdFromPath(String path) {
    if (path == null) {
      return Optional.empty();
    }
    Matcher m = ATTACHMENT_IMAGE_PATH_PREFIX.matcher(path.trim());
    if (!m.find() || m.start() != 0) {
      return Optional.empty();
    }
    return Optional.of(Integer.parseInt(m.group(1)));
  }

  private static String stripSurroundingQuotes(String s) {
    if (s.length() >= 2
        && ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'")))) {
      return s.substring(1, s.length() - 1).trim();
    }
    return s;
  }

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

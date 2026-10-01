package com.odde.donut.algorithms;

import com.odde.donut.entities.PropertyFocus;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Note content helpers for the leading YAML block: orchestrates fence parsing ({@link
 * NoteLeadingFrontmatter}), parsed properties ({@link Frontmatter}) and in-place property writes
 * ({@link FrontmatterInPlaceEdit}).
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

  /** The leading frontmatter's {@code image:} scalar, when present. */
  public static Optional<String> noteImage(String content) {
    return splitLeadingFrontmatter(content)
        .flatMap(lf -> lf.frontmatter().getString(NOTE_IMAGE_KEY));
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
    UnaryOperator<String> removeLinks =
        v -> linkTexts.stream().reduce(v, (s, t) -> s.replace("[[" + t + "]]", ""));
    return NoteLeadingFrontmatter.splitVerbatim(content)
        .flatMap(
            split -> {
              String yaml =
                  FrontmatterInPlaceEdit.rewriteSupportedValues(split.yamlRaw(), removeLinks);
              if (yaml.equals(split.yamlRaw())) {
                return Optional.empty();
              }
              return Optional.of(rebuildDroppingEmptyBlock(split, yaml));
            });
  }

  /**
   * Removes the first {@code key} property (case-insensitive) from the leading frontmatter,
   * dropping the block when it was the last property; every other line stays as is.
   */
  public static String removeFrontmatterProperty(String content, String key) {
    return NoteLeadingFrontmatter.splitVerbatim(content)
        .map(
            split ->
                rebuildDroppingEmptyBlock(
                    split, FrontmatterInPlaceEdit.removeTopLevelEntry(split.yamlRaw(), key)))
        .orElse(content);
  }

  private static String rebuildDroppingEmptyBlock(
      NoteLeadingFrontmatter.VerbatimSplit split, String yaml) {
    return Frontmatter.parse(yaml).isEmpty() ? split.body() : split.rebuild(yaml);
  }

  public record ConsolidatedProperties(
      String content, Map<PropertyFocus, PropertyFocus> focuses, String diagnostic) {}

  /** Returns exact source-to-list focuses, or a diagnostic without transformed content. */
  public static ConsolidatedProperties consolidateNumberedProperties(String content) {
    if (content == null) {
      return new ConsolidatedProperties(null, Map.of(), null);
    }
    var split = NoteLeadingFrontmatter.splitPreservingSource(content);
    if (split.isEmpty()) {
      return new ConsolidatedProperties(content, Map.of(), null);
    }
    var result = FrontmatterInPlaceEdit.consolidateNumberedProperties(split.get().yamlRaw());
    if (result.diagnostic() != null) {
      return new ConsolidatedProperties(null, Map.of(), result.diagnostic());
    }
    return new ConsolidatedProperties(split.get().rebuild(result.yaml()), result.focuses(), null);
  }

  /**
   * The content after {@link #addPropertyValueToLeadingFrontmatter}, the key as authored, the value
   * a tracker of the added value focuses on ('' for a single value), and the former single value
   * that became a list item (null when none did).
   */
  public record AddedPropertyValue(
      String content, String key, String trackedValue, String formerSingleValue) {}

  /**
   * Adds {@code value} to {@code key} (matched case-insensitively): a key holding values gets it as
   * one more list item (a single value becomes a list; a value already there is not repeated);
   * otherwise (absent or blank) the key is set to it as a single value. Empty when the key holds an
   * unsupported value such as a map, which is left as authored.
   */
  public static Optional<AddedPropertyValue> addPropertyValueToLeadingFrontmatter(
      String content, String key, String value) {
    Frontmatter frontmatter =
        splitLeadingFrontmatter(content == null ? "" : content)
            .map(LeadingFrontmatter::frontmatter)
            .orElse(Frontmatter.empty());
    if (frontmatter.holdsUnsupportedValue(key)) {
      return Optional.empty();
    }
    String authoredKey =
        frontmatter.keys().stream().filter(key::equalsIgnoreCase).findFirst().orElse(key);
    return Optional.of(
        switch (frontmatter.getPropertyValue(key).orElse(null)) {
          case FrontmatterPropertyValue.ListItems list when list.items().contains(value) ->
              new AddedPropertyValue(content, authoredKey, value, null);
          case FrontmatterPropertyValue.ListItems ignored ->
              new AddedPropertyValue(
                  addListItem(content, authoredKey, value), authoredKey, value, null);
          case FrontmatterPropertyValue.Scalar single when single.value().equals(value) ->
              new AddedPropertyValue(content, authoredKey, "", null);
          case FrontmatterPropertyValue.Scalar single when !single.value().isBlank() ->
              new AddedPropertyValue(
                  addListItem(content, authoredKey, value), authoredKey, value, single.value());
          case null, default ->
              new AddedPropertyValue(
                  setLeadingFrontmatterProperty(content, authoredKey, value),
                  authoredKey,
                  "",
                  null);
        });
  }

  private static String addListItem(String content, String key, String value) {
    return NoteLeadingFrontmatter.splitVerbatim(content)
        .map(
            split ->
                split.rebuild(
                    FrontmatterInPlaceEdit.addTopLevelListItem(split.yamlRaw(), key, value)))
        .orElseThrow();
  }

  /**
   * Sets a scalar property in the first leading YAML frontmatter block, replacing an existing
   * {@code key} (case-insensitive) in place or appending it, and creates the block when absent.
   */
  public static String setLeadingFrontmatterProperty(String content, String key, String value) {
    String text = content == null ? "" : content;
    return NoteLeadingFrontmatter.splitVerbatim(text)
        .map(
            split ->
                split.rebuild(
                    FrontmatterInPlaceEdit.setTopLevelScalar(split.yamlRaw(), key, value)))
        .orElseGet(() -> Frontmatter.empty().set(key, value).fenced(text));
  }
}

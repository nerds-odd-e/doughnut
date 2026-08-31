package com.odde.donut.algorithms;

import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Authored wiki or path-Markdown link target, eagerly split into its three parts: an optional
 * notebook qualifier (present only when literally written, e.g. {@code Notebook:Title}), the note
 * portion (shorthand title or path-shaped spelling, optionally with a trailing {@code .md}), and an
 * optional encoded {@code #prop:} property selector (ADR 0004). The decoded property key is the
 * same YAML property-key string used by {@link PropertyKeyNaming} and {@code
 * note_property_index.property_key}.
 *
 * <p>The notebook qualifier here is a purely syntactic split of the authored text, not the
 * focus-notebook fallback used at resolution time: see {@link #resolve}.
 */
public record PortablePath(
    Optional<String> notebookQualifier, String notePortion, Optional<String> encodedPropertyKey) {

  private static final String PROPERTY_SEPARATOR = "#prop:";

  public static PortablePath parse(String raw) {
    BeforePropertySplit split = BeforePropertySplit.of(raw);
    Qualified qualified = Qualified.tryParse(split.beforeProp());
    if (qualified != null) {
      return new PortablePath(
          Optional.of(qualified.notebookName()), qualified.noteTitle(), split.encodedPropertyKey());
    }
    return new PortablePath(Optional.empty(), split.beforeProp(), split.encodedPropertyKey());
  }

  /**
   * Transforms the raw text before any encoded {@code #prop:} suffix, without parsing out a
   * notebook qualifier. Some callers (OS-invalid-character sanitization) apply their own qualifier
   * heuristic to that raw text; this reattaches the property suffix unchanged.
   */
  public static String mapBeforePropertySuffix(String raw, UnaryOperator<String> transform) {
    BeforePropertySplit split = BeforePropertySplit.of(raw);
    String converted = transform.apply(split.beforeProp());
    return split
        .encodedPropertyKey()
        .map(key -> converted + PROPERTY_SEPARATOR + key)
        .orElse(converted);
  }

  private record BeforePropertySplit(String beforeProp, Optional<String> encodedPropertyKey) {
    static BeforePropertySplit of(String raw) {
      String text = raw == null ? "" : raw;
      int separator = text.indexOf(PROPERTY_SEPARATOR);
      if (separator < 0) {
        return new BeforePropertySplit(text, Optional.empty());
      }
      return new BeforePropertySplit(
          text.substring(0, separator),
          Optional.of(text.substring(separator + PROPERTY_SEPARATOR.length())));
    }
  }

  public String format() {
    String base = notebookQualifier.map(nb -> nb + ":" + notePortion).orElse(notePortion);
    return encodedPropertyKey.map(key -> base + PROPERTY_SEPARATOR + key).orElse(base);
  }

  /** The qualifier and note portion, without the encoded property suffix. */
  public String qualifiedNotePortion() {
    return notebookQualifier.map(nb -> nb + ":" + notePortion).orElse(notePortion);
  }

  /**
   * Transforms the qualifier-and-note-portion text as one opaque string (not re-split into
   * qualifier/note-portion), keeping the encoded property suffix untouched.
   */
  public PortablePath mapQualifiedNotePortion(UnaryOperator<String> transform) {
    return new PortablePath(
        Optional.empty(), transform.apply(qualifiedNotePortion()), encodedPropertyKey);
  }

  public boolean hasPropertySuffix() {
    return encodedPropertyKey.isPresent();
  }

  public boolean hasNotebookQualifier() {
    return notebookQualifier.isPresent();
  }

  public Optional<String> decodedPropertyKey() {
    return encodedPropertyKey.flatMap(PropertyKeyPercentEncoding::decode);
  }

  /**
   * Resolves the notebook name and note title to look up, applying the focus-notebook fallback for
   * unqualified references. Empty when unqualified with a blank note portion, or unqualified with
   * no focus notebook available.
   */
  public Optional<Resolved> resolve(String focusNotebookName) {
    if (notebookQualifier.isPresent()) {
      return Optional.of(new Resolved(notebookQualifier.get(), notePortion));
    }
    if (notePortion.isBlank()) {
      return Optional.empty();
    }
    if (focusNotebookName == null || focusNotebookName.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(new Resolved(focusNotebookName, notePortion));
  }

  /** Notebook name and note title to resolve a wiki-link to a target note. */
  public record Resolved(String notebookName, String noteTitle) {}

  public PortablePath withNoteTitle(String newTitle) {
    if (notebookQualifier.isPresent()) {
      return new PortablePath(notebookQualifier, newTitle, encodedPropertyKey);
    }
    String newPortion =
        PathShapedTarget.tryParse(notePortion)
            .map(path -> path.withNoteTitle(newTitle))
            .orElse(newTitle);
    return new PortablePath(Optional.empty(), newPortion, encodedPropertyKey);
  }

  public PortablePath withRenamedFolder(String oldFolderName, String newFolderName) {
    if (notebookQualifier.isPresent()) {
      return this;
    }
    String newPortion =
        PathShapedTarget.tryParse(notePortion)
            .map(path -> path.withRenamedFolder(oldFolderName, newFolderName))
            .orElse(notePortion);
    return new PortablePath(Optional.empty(), newPortion, encodedPropertyKey);
  }

  public PortablePath withNotebookName(String newNotebookName) {
    return new PortablePath(Optional.of(newNotebookName), notePortion, encodedPropertyKey);
  }

  static String replaceNoteTitle(String authoredToken, String newTitle) {
    return parse(authoredToken).withNoteTitle(newTitle).format();
  }

  static String replaceFolderName(
      String authoredToken, String oldFolderName, String newFolderName) {
    return parse(authoredToken).withRenamedFolder(oldFolderName, newFolderName).format();
  }

  static String replaceNotebookName(String authoredToken, String newNotebookName) {
    return parse(authoredToken).withNotebookName(newNotebookName).format();
  }

  static boolean isQualifiedToken(String authoredToken) {
    return parse(authoredToken).hasNotebookQualifier();
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

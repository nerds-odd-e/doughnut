package com.odde.doughnut.services;

public final class LegacyParentWikiFrontmatterMerge {

  private static final String UNTITLED = "Untitled";

  private LegacyParentWikiFrontmatterMerge() {}

  /**
   * Adds {@code parent: "[[displayTitle]]"} to YAML frontmatter when missing; preserves existing
   * frontmatter keys and body. Returns {@code details} unchanged when {@code parent} is already
   * declared.
   */
  public static String mergeParentWikiLink(String details, String parentTitleOrNull) {
    String displayTitle = displayTitle(parentTitleOrNull);
    String wikiToken = "[[" + displayTitle + "]]";
    String parentLine = "parent: \"" + yamlDoubleQuotedInner(wikiToken) + "\"";

    if (details == null || details.isBlank()) {
      return "---\n" + parentLine + "\n---\n\n";
    }
    String original = details;
    String n = original.replace("\r\n", "\n");
    if (!n.startsWith("---\n")) {
      return "---\n" + parentLine + "\n---\n\n" + n;
    }
    int sep = n.indexOf("\n---\n\n", 1);
    if (sep < 0) {
      return "---\n" + parentLine + "\n---\n\n" + n;
    }
    String frontmatterBlock = n.substring(4, sep);
    if (frontmatterDeclaresParent(frontmatterBlock)) {
      return original;
    }
    String body = n.substring(sep + 6);
    String newFm =
        frontmatterBlock.isEmpty()
            ? parentLine
            : frontmatterBlock.endsWith("\n")
                ? frontmatterBlock + parentLine
                : frontmatterBlock + "\n" + parentLine;
    return "---\n" + newFm + "\n---\n\n" + body;
  }

  private static String displayTitle(String title) {
    if (title == null) {
      return UNTITLED;
    }
    String t = title.trim();
    return t.isEmpty() ? UNTITLED : t;
  }

  private static boolean frontmatterDeclaresParent(String frontmatterBlock) {
    for (String raw : frontmatterBlock.split("\n")) {
      String line = raw.stripLeading();
      int c = line.indexOf(':');
      if (c <= 0) {
        continue;
      }
      String key = line.substring(0, c).trim();
      if ("parent".equals(key)) {
        return true;
      }
    }
    return false;
  }

  private static String yamlDoubleQuotedInner(String s) {
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}

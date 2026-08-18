package com.odde.doughnut.algorithms;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/** Leading {@code ---} fenced block at the start of note markdown: split and rebuild. */
public final class NoteLeadingFrontmatter {

  private static final Pattern TOP_LEVEL_TYPE_LINE =
      Pattern.compile("(?im)^(type\\s*:)([ \\t]*)(?:\"[^\"]*\"|'[^']*'|\\S+)?[ \\t]*$(\\n)?");

  private NoteLeadingFrontmatter() {}

  public record Split(Frontmatter frontmatter, String body) {}

  /**
   * The leading block as the author wrote it — fences included, nothing reparsed or re-dumped —
   * with the YAML between the fences and the body after it. Reading a property means {@link
   * #split}; handing the block back to the author means this, because a round trip through {@link
   * Frontmatter} settles comments, key order, and quoting for them.
   */
  public record VerbatimSplit(String frontmatterBlock, String yamlRaw, String body) {
    String rebuild(String newYamlRaw) {
      String yaml =
          newYamlRaw.isEmpty() || newYamlRaw.endsWith("\n") ? newYamlRaw : newYamlRaw + "\n";
      return "---\n" + yaml + "---\n" + body;
    }
  }

  /**
   * Ensures a top-level {@code type} key without re-dumping the rest of the fence. Missing or blank
   * type becomes {@code typeWhenAbsent} as the first key. A present type matching a canonical
   * spelling (case-insensitive) is rewritten in place; any other non-empty type is left unchanged.
   */
  public static String ensureTypeKey(
      String content, String typeWhenAbsent, String... canonicalSpellings) {
    Optional<VerbatimSplit> verbatim = splitVerbatim(content);
    if (verbatim.isEmpty()) {
      String body = content == null ? "" : content;
      return "---\ntype: " + typeWhenAbsent + "\n---\n" + body;
    }
    VerbatimSplit split = verbatim.get();
    Optional<String> type =
        Frontmatter.parse(split.yamlRaw()).getString("type").filter(s -> !s.isBlank());
    if (type.isEmpty()) {
      return insertTypeFirst(split, typeWhenAbsent);
    }
    return canonicalizeTypeSpelling(content, split, type.get(), canonicalSpellings);
  }

  /**
   * Appends a top-level {@code title} key without re-dumping the rest of the fence. A present title
   * (case-insensitive) is left unchanged. Missing fence or missing title key gets {@code title:}
   * appended, YAML-quoted as needed.
   */
  public static String ensureTitleKey(String content, String title) {
    Optional<VerbatimSplit> verbatim = splitVerbatim(content);
    if (verbatim.isPresent()
        && Frontmatter.parse(verbatim.get().yamlRaw()).containsKeyIgnoreCase("title")) {
      return content;
    }
    VerbatimSplit split =
        verbatim.orElseGet(() -> new VerbatimSplit("", "", content == null ? "" : content));
    String yaml = split.yamlRaw();
    String withNewline = yaml.isEmpty() || yaml.endsWith("\n") ? yaml : yaml + "\n";
    return split.rebuild(withNewline + titleKeyLine(title) + "\n");
  }

  public static Optional<Split> split(String content) {
    return splitVerbatim(content).map(s -> new Split(Frontmatter.parse(s.yamlRaw()), s.body()));
  }

  /**
   * Prepends {@code addition} to the markdown body. A closed leading fence stays in place; unfenced
   * content is {@code addition} then previous, separated by a blank line.
   */
  public static String prependToBody(String content, String addition) {
    String prev = content == null ? "" : content;
    Optional<VerbatimSplit> verbatim = splitVerbatim(prev);
    String prefix = verbatim.map(s -> s.frontmatterBlock() + "\n").orElse("");
    String rest = verbatim.map(VerbatimSplit::body).orElse(prev);
    String merged = rest.isEmpty() ? addition : addition + "\n\n" + rest;
    return prefix + merged;
  }

  public static Optional<VerbatimSplit> splitVerbatim(String content) {
    if (content == null || content.isEmpty()) {
      return Optional.empty();
    }
    String normalized = content.replace("\r\n", "\n").replace('\r', '\n');
    String work = stripUtf8Bom(normalized);
    String[] lines = work.split("\n", -1);
    if (lines.length == 0 || !"---".equals(lines[0])) {
      return Optional.empty();
    }
    for (int i = 1; i < lines.length; i++) {
      if ("---".equals(lines[i])) {
        String yamlRaw = joinLines(lines, 1, i);
        String body = joinLines(lines, i + 1, lines.length);
        String frontmatterBlock = joinLines(lines, 0, i + 1);
        return Optional.of(new VerbatimSplit(frontmatterBlock, yamlRaw, body));
      }
    }
    return Optional.empty();
  }

  private static String insertTypeFirst(VerbatimSplit split, String typeWhenAbsent) {
    String remainder = TOP_LEVEL_TYPE_LINE.matcher(split.yamlRaw()).replaceFirst("");
    return split.rebuild("type: " + typeWhenAbsent + "\n" + remainder);
  }

  private static String canonicalizeTypeSpelling(
      String content, VerbatimSplit split, String type, String[] canonicalSpellings) {
    Optional<String> canonical = canonicalSpelling(type.trim(), canonicalSpellings);
    if (canonical.isEmpty()) {
      return content;
    }
    String newYaml =
        TOP_LEVEL_TYPE_LINE.matcher(split.yamlRaw()).replaceFirst("$1$2" + canonical.get() + "$3");
    if (newYaml.equals(split.yamlRaw())) {
      return content;
    }
    return split.rebuild(newYaml);
  }

  private static String titleKeyLine(String title) {
    DumperOptions opts = new DumperOptions();
    opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    opts.setExplicitStart(false);
    opts.setExplicitEnd(false);
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("title", title == null ? "" : title);
    return new Yaml(opts).dump(map).strip();
  }

  private static Optional<String> canonicalSpelling(String type, String[] canonicalSpellings) {
    for (String spelling : canonicalSpellings) {
      if (spelling.equalsIgnoreCase(type)) {
        return Optional.of(spelling);
      }
    }
    return Optional.empty();
  }

  private static String stripUtf8Bom(String s) {
    if (!s.isEmpty() && s.charAt(0) == '\uFEFF') {
      return s.substring(1);
    }
    return s;
  }

  private static String joinLines(String[] lines, int fromIndex, int toIndex) {
    if (fromIndex >= toIndex) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int j = fromIndex; j < toIndex; j++) {
      if (j > fromIndex) {
        sb.append('\n');
      }
      sb.append(lines[j]);
    }
    return sb.toString();
  }
}

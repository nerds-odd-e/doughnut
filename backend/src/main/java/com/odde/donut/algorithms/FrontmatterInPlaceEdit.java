package com.odde.donut.algorithms;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

/**
 * Edits the YAML between the frontmatter fences in place: only the affected source ranges (from
 * SnakeYAML node marks) are replaced, so untouched entries keep their comments, order and quoting.
 */
public final class FrontmatterInPlaceEdit {

  private FrontmatterInPlaceEdit() {}

  /**
   * Rewrites supported values — top-level scalars and all-scalar sequence items — with {@code
   * rewrite}. The rewrite is applied to the value's source text when that keeps it valid; otherwise
   * the rewritten value is written in the value's scalar style. When the rewrite empties a value,
   * an entry left with no non-blank value is removed whole, otherwise the blank items are cut from
   * their sequence.
   */
  public static String rewriteSupportedValues(String yamlRaw, UnaryOperator<String> rewrite) {
    Node document = new Yaml().compose(new StringReader(yamlRaw));
    if (!(document instanceof MappingNode mapping)) {
      return yamlRaw;
    }
    List<Replacement> replacements = new ArrayList<>();
    for (NodeTuple tuple : mapping.getValue()) {
      List<ScalarNode> scalars = supportedScalars(tuple.getValueNode());
      if (scalars.stream().noneMatch(scalar -> emptiedBy(scalar, rewrite))) {
        scalars.forEach(scalar -> rewriteScalar(yamlRaw, scalar, rewrite, replacements));
      } else if (scalars.stream().allMatch(scalar -> rewrite.apply(scalar.getValue()).isBlank())) {
        replacements.add(entryRemoval(yamlRaw, tuple, scalars.getLast()));
      } else {
        replacements.addAll(itemRemovals(yamlRaw, scalars, rewrite));
        scalars.stream()
            .filter(scalar -> !rewrite.apply(scalar.getValue()).isBlank())
            .forEach(scalar -> rewriteScalar(yamlRaw, scalar, rewrite, replacements));
      }
    }
    return splice(yamlRaw, replacements);
  }

  private static void rewriteScalar(
      String yamlRaw,
      ScalarNode scalar,
      UnaryOperator<String> rewrite,
      List<Replacement> replacements) {
    String transformed = rewrite.apply(scalar.getValue());
    if (transformed.equals(scalar.getValue())) {
      return;
    }
    int start = offset(yamlRaw, scalar.getStartMark());
    int end = offset(yamlRaw, scalar.getEndMark());
    String source = yamlRaw.substring(start, end);
    String rewrittenSource = rewrite.apply(source);
    if (scalar.getScalarStyle() == DumperOptions.ScalarStyle.DOUBLE_QUOTED
        || rewrittenSource.equals(source)) {
      rewrittenSource = dumpScalar(transformed, scalar.getScalarStyle());
    }
    replacements.add(new Replacement(start, end, rewrittenSource));
  }

  /**
   * Sets a top-level {@code key} (matched case-insensitively) to the scalar {@code value}: the
   * existing entry's text is replaced, otherwise the entry is appended after the last line.
   */
  public static String setTopLevelScalar(String yamlRaw, String key, String value) {
    String entry = dumpEntry(key, value);
    Node document = new Yaml().compose(new StringReader(yamlRaw));
    if (document instanceof MappingNode mapping) {
      for (NodeTuple tuple : mapping.getValue()) {
        if (tuple.getKeyNode() instanceof ScalarNode keyNode
            && keyNode.getValue().equalsIgnoreCase(key)) {
          int start = offset(yamlRaw, keyNode.getStartMark());
          int end = offset(yamlRaw, tuple.getValueNode().getEndMark());
          return splice(yamlRaw, List.of(new Replacement(start, end, entry)));
        }
      }
    }
    return yamlRaw.isEmpty() ? entry : yamlRaw + "\n" + entry;
  }

  private static boolean emptiedBy(ScalarNode scalar, UnaryOperator<String> rewrite) {
    return !scalar.getValue().isBlank() && rewrite.apply(scalar.getValue()).isBlank();
  }

  /** From the key's line start through the end of the line holding {@code last}. */
  private static Replacement entryRemoval(String yamlRaw, NodeTuple tuple, ScalarNode last) {
    int start =
        yamlRaw.lastIndexOf('\n', offset(yamlRaw, tuple.getKeyNode().getStartMark()) - 1) + 1;
    int lineEnd = yamlRaw.indexOf('\n', offset(yamlRaw, last.getEndMark()));
    return new Replacement(start, lineEnd < 0 ? yamlRaw.length() : lineEnd + 1, "");
  }

  /**
   * Each run of emptied items is cut up to the next kept item's start; a trailing run is cut from
   * the last kept item's end, so block and flow sequences both stay well formed.
   */
  private static List<Replacement> itemRemovals(
      String yamlRaw, List<ScalarNode> items, UnaryOperator<String> rewrite) {
    List<Replacement> removals = new ArrayList<>();
    Integer runStart = null;
    ScalarNode lastKept = null;
    for (ScalarNode item : items) {
      if (rewrite.apply(item.getValue()).isBlank()) {
        if (runStart == null) {
          runStart = offset(yamlRaw, item.getStartMark());
        }
      } else {
        if (runStart != null) {
          removals.add(new Replacement(runStart, offset(yamlRaw, item.getStartMark()), ""));
          runStart = null;
        }
        lastKept = item;
      }
    }
    if (runStart != null) {
      removals.add(
          new Replacement(
              offset(yamlRaw, lastKept.getEndMark()),
              offset(yamlRaw, items.getLast().getEndMark()),
              ""));
    }
    return removals;
  }

  private static String dumpEntry(String key, String value) {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    return stripTrailingNewline(new Yaml(options).dump(Map.of(key, value)));
  }

  private static List<ScalarNode> supportedScalars(Node value) {
    if (value instanceof ScalarNode scalar) {
      return List.of(scalar);
    }
    if (value instanceof SequenceNode sequence
        && sequence.getValue().stream().allMatch(ScalarNode.class::isInstance)) {
      return sequence.getValue().stream().map(ScalarNode.class::cast).toList();
    }
    return List.of();
  }

  /** SnakeYAML marks count code points; {@link String} offsets count UTF-16 chars. */
  private static int offset(String yamlRaw, Mark mark) {
    return yamlRaw.offsetByCodePoints(0, mark.getIndex());
  }

  private static String splice(String yamlRaw, List<Replacement> replacements) {
    StringBuilder rewritten = new StringBuilder(yamlRaw);
    replacements.stream()
        .sorted(Comparator.comparingInt(Replacement::start).reversed())
        .forEach(r -> rewritten.replace(r.start(), r.end(), r.text()));
    return rewritten.toString();
  }

  private static String dumpScalar(String value, DumperOptions.ScalarStyle style) {
    DumperOptions options = new DumperOptions();
    options.setDefaultScalarStyle(style);
    options.setSplitLines(false);
    return stripTrailingNewline(new Yaml(options).dump(value));
  }

  private static String stripTrailingNewline(String dumped) {
    return dumped.endsWith("\n") ? dumped.substring(0, dumped.length() - 1) : dumped;
  }

  private record Replacement(int start, int end, String text) {}
}

package com.odde.donut.algorithms;

import static com.odde.donut.algorithms.YamlSourceEdit.dumpEntry;
import static com.odde.donut.algorithms.YamlSourceEdit.dumpScalar;
import static com.odde.donut.algorithms.YamlSourceEdit.entryRemoval;
import static com.odde.donut.algorithms.YamlSourceEdit.offset;
import static com.odde.donut.algorithms.YamlSourceEdit.splice;

import com.odde.donut.algorithms.YamlSourceEdit.Replacement;
import com.odde.donut.entities.PropertyFocus;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
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

  /** A diagnostic has no transformed YAML or focus mapping. */
  public record ConsolidatedProperties(
      String yaml,
      Map<PropertyFocus, PropertyFocus> focuses,
      Set<String> sourceKeys,
      String diagnostic) {}

  /** Consolidates exact authored numeric families without rewriting other source ranges. */
  public static ConsolidatedProperties consolidateNumberedProperties(String yamlRaw) {
    return FrontmatterNumberedProperties.consolidate(yamlRaw);
  }

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
    return topLevelEntry(yamlRaw, key)
        .map(
            tuple ->
                splice(
                    yamlRaw,
                    List.of(
                        new Replacement(
                            offset(yamlRaw, tuple.getKeyNode().getStartMark()),
                            offset(yamlRaw, tuple.getValueNode().getEndMark()),
                            entry))))
        .orElseGet(() -> yamlRaw.isEmpty() ? entry : yamlRaw + "\n" + entry);
  }

  /**
   * Removes the first top-level {@code key} entry (matched case-insensitively), with its whole
   * value, leaving every other line as is.
   */
  public static String removeTopLevelEntry(String yamlRaw, String key) {
    return topLevelEntry(yamlRaw, key)
        .map(tuple -> splice(yamlRaw, List.of(entryRemoval(yamlRaw, tuple, tuple.getValueNode()))))
        .orElse(yamlRaw);
  }

  /**
   * Adds {@code value} as a list item of the top-level {@code key} (matched case-insensitively),
   * which holds a scalar or an all-scalar sequence: a sequence gets the item appended in its own
   * style, a scalar becomes a flow list of itself and the item.
   */
  public static String addTopLevelListItem(String yamlRaw, String key, String value) {
    Node node = topLevelEntry(yamlRaw, key).orElseThrow().getValueNode();
    String item = dumpScalar(value, DumperOptions.ScalarStyle.DOUBLE_QUOTED);
    int start = offset(yamlRaw, node.getStartMark());
    int end = offset(yamlRaw, node.getEndMark());
    Replacement addition =
        switch (node) {
          case SequenceNode sequence
              when sequence.getFlowStyle() == DumperOptions.FlowStyle.BLOCK -> {
            int lastStart = offset(yamlRaw, sequence.getValue().getLast().getStartMark());
            int lastEnd = offset(yamlRaw, sequence.getValue().getLast().getEndMark());
            String itemPrefix =
                yamlRaw.substring(yamlRaw.lastIndexOf('\n', lastStart - 1) + 1, lastStart);
            yield new Replacement(lastEnd, lastEnd, "\n" + itemPrefix + item);
          }
          case SequenceNode sequence -> {
            int close = yamlRaw.lastIndexOf(']', end - 1);
            yield new Replacement(close, close, (sequence.getValue().isEmpty() ? "" : ", ") + item);
          }
          case ScalarNode scalar ->
              new Replacement(
                  start,
                  end,
                  "["
                      + dumpScalar(scalar.getValue(), DumperOptions.ScalarStyle.DOUBLE_QUOTED)
                      + ", "
                      + item
                      + "]");
          default -> throw new IllegalArgumentException("Not a scalar or list: " + key);
        };
    return splice(yamlRaw, List.of(addition));
  }

  /** The top-level entry whose scalar key matches {@code key} case-insensitively. */
  private static Optional<NodeTuple> topLevelEntry(String yamlRaw, String key) {
    if (!(new Yaml().compose(new StringReader(yamlRaw)) instanceof MappingNode mapping)) {
      return Optional.empty();
    }
    return mapping.getValue().stream()
        .filter(
            tuple ->
                tuple.getKeyNode() instanceof ScalarNode keyNode
                    && keyNode.getValue().equalsIgnoreCase(key))
        .findFirst();
  }

  private static boolean emptiedBy(ScalarNode scalar, UnaryOperator<String> rewrite) {
    return !scalar.getValue().isBlank() && rewrite.apply(scalar.getValue()).isBlank();
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
}

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
   * the rewritten value is written in the value's scalar style.
   */
  public static String rewriteSupportedScalars(String yamlRaw, UnaryOperator<String> rewrite) {
    Node document = new Yaml().compose(new StringReader(yamlRaw));
    if (!(document instanceof MappingNode mapping)) {
      return yamlRaw;
    }
    List<Replacement> replacements = new ArrayList<>();
    for (ScalarNode scalar : supportedScalars(mapping)) {
      String transformed = rewrite.apply(scalar.getValue());
      if (transformed.equals(scalar.getValue())) {
        continue;
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
    return splice(yamlRaw, replacements);
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

  private static String dumpEntry(String key, String value) {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    return stripTrailingNewline(new Yaml(options).dump(Map.of(key, value)));
  }

  private static List<ScalarNode> supportedScalars(MappingNode mapping) {
    List<ScalarNode> scalars = new ArrayList<>();
    mapping
        .getValue()
        .forEach(
            tuple -> {
              Node value = tuple.getValueNode();
              if (value instanceof ScalarNode scalar) {
                scalars.add(scalar);
              } else if (value instanceof SequenceNode sequence
                  && sequence.getValue().stream().allMatch(ScalarNode.class::isInstance)) {
                sequence.getValue().forEach(item -> scalars.add((ScalarNode) item));
              }
            });
    return scalars;
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

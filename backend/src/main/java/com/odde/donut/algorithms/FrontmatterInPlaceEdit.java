package com.odde.donut.algorithms;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
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
      int start = yamlRaw.offsetByCodePoints(0, scalar.getStartMark().getIndex());
      int end = yamlRaw.offsetByCodePoints(0, scalar.getEndMark().getIndex());
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
    String dumped = new Yaml(options).dump(value);
    return dumped.endsWith("\n") ? dumped.substring(0, dumped.length() - 1) : dumped;
  }

  private record Replacement(int start, int end, String text) {}
}

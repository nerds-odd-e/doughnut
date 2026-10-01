package com.odde.donut.algorithms;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;

/** Source ranges and scalar emission shared by in-place YAML edits. */
final class YamlSourceEdit {
  private YamlSourceEdit() {}

  /**
   * From the key's line start through the end of the line holding {@code last}'s final character (a
   * block node's end mark already sits at the next line's start).
   */
  static Replacement entryRemoval(String yamlRaw, NodeTuple tuple, Node last) {
    int start =
        yamlRaw.lastIndexOf('\n', offset(yamlRaw, tuple.getKeyNode().getStartMark()) - 1) + 1;
    int lineEnd = yamlRaw.indexOf('\n', offset(yamlRaw, last.getEndMark()) - 1);
    return new Replacement(start, lineEnd < 0 ? yamlRaw.length() : lineEnd + 1, "");
  }

  static String dumpEntry(String key, String value) {
    DumperOptions options = new DumperOptions();
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    return stripTrailingNewline(new Yaml(options).dump(Map.of(key, value)));
  }

  /** SnakeYAML marks count code points; {@link String} offsets count UTF-16 chars. */
  static int offset(String yamlRaw, Mark mark) {
    return yamlRaw.offsetByCodePoints(0, mark.getIndex());
  }

  static String splice(String yamlRaw, List<Replacement> replacements) {
    StringBuilder rewritten = new StringBuilder(yamlRaw);
    replacements.stream()
        .sorted(Comparator.comparingInt(Replacement::start).reversed())
        .forEach(r -> rewritten.replace(r.start(), r.end(), r.text()));
    return rewritten.toString();
  }

  static String dumpScalar(String value, DumperOptions.ScalarStyle style) {
    DumperOptions options = new DumperOptions();
    options.setDefaultScalarStyle(style);
    options.setSplitLines(false);
    return stripTrailingNewline(new Yaml(options).dump(value));
  }

  private static String stripTrailingNewline(String dumped) {
    return dumped.endsWith("\n") ? dumped.substring(0, dumped.length() - 1) : dumped;
  }

  record Replacement(int start, int end, String text) {}
}

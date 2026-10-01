package com.odde.donut.algorithms;

import static com.odde.donut.algorithms.YamlSourceEdit.dumpScalar;
import static com.odde.donut.algorithms.YamlSourceEdit.entryRemoval;
import static com.odde.donut.algorithms.YamlSourceEdit.offset;
import static com.odde.donut.algorithms.YamlSourceEdit.splice;

import com.odde.donut.algorithms.FrontmatterInPlaceEdit.ConsolidatedProperties;
import com.odde.donut.algorithms.YamlSourceEdit.Replacement;
import com.odde.donut.entities.PropertyFocus;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;

/** Exact authored numbered families and their source-to-list focus mapping. */
final class FrontmatterNumberedProperties {
  private FrontmatterNumberedProperties() {}

  /** Consolidates exact authored numeric families without rewriting other source ranges. */
  static ConsolidatedProperties consolidate(String yamlRaw) {
    try {
      Node document = new Yaml().compose(new StringReader(yamlRaw));
      if (!(document instanceof MappingNode mapping)) {
        return new ConsolidatedProperties(null, Map.of(), Set.of(), "Frontmatter is not a mapping");
      }
      Object loaded = new Yaml().load(yamlRaw);
      Map<?, ?> values = (Map<?, ?>) loaded;
      Map<String, NodeTuple> entries = new LinkedHashMap<>();
      for (NodeTuple tuple : mapping.getValue()) {
        if (!(tuple.getKeyNode() instanceof ScalarNode key)
            || entries.putIfAbsent(key.getValue(), tuple) != null) {
          return new ConsolidatedProperties(
              null, Map.of(), Set.of(), "Ambiguous authored property key");
        }
      }
      Map<String, List<String>> families = new LinkedHashMap<>();
      for (String key : entries.keySet()) {
        var parts = PropertyKeyNaming.propertyKeyBaseAndSuffix(key);
        if (parts.suffix() != null && PropertyKeyNaming.isListCapablePropertyKey(parts.base())) {
          families.computeIfAbsent(parts.base(), ignored -> new ArrayList<>()).add(key);
        }
      }
      if (families.isEmpty()) {
        return new ConsolidatedProperties(yamlRaw, Map.of(), Set.of(), null);
      }
      if (mapping.getFlowStyle() == DumperOptions.FlowStyle.FLOW) {
        return new ConsolidatedProperties(
            null, Map.of(), Set.of(), "Flow mapping cannot be edited in place");
      }
      List<Replacement> replacements = new ArrayList<>();
      Map<PropertyFocus, PropertyFocus> focuses = new LinkedHashMap<>();
      Set<String> sourceKeys = new LinkedHashSet<>();
      Map<Object, Object> expected = new LinkedHashMap<>(values);
      for (var family : families.entrySet()) {
        String base = family.getKey();
        List<String> keys = family.getValue();
        keys.sort(
            Comparator.comparing(k -> PropertyKeyNaming.propertyKeyBaseAndSuffix(k).suffix()));
        if (entries.containsKey(base)) {
          keys.addFirst(base);
        }
        LinkedHashSet<String> items = new LinkedHashSet<>();
        sourceKeys.addAll(keys);
        for (String key : keys) {
          var value = FrontmatterPropertyValues.fromYamlObject(values.get(key));
          if (value.isEmpty()) {
            return new ConsolidatedProperties(
                null, Map.of(), Set.of(), "Unsupported property value: " + key);
          }
          List<String> sourceItems =
              switch (value.get()) {
                case FrontmatterPropertyValue.Scalar scalar -> List.of(scalar.value());
                case FrontmatterPropertyValue.ListItems list -> list.items();
              };
          for (String item : sourceItems) {
            items.add(item);
            String sourceValue = value.get() instanceof FrontmatterPropertyValue.Scalar ? "" : item;
            focuses.put(new PropertyFocus(key, sourceValue), new PropertyFocus(base, item));
          }
          expected.remove(key);
        }
        expected.put(base, List.copyOf(items));
        NodeTuple retained = entries.get(keys.getFirst());
        String list =
            "["
                + items.stream()
                    .map(v -> dumpScalar(v, DumperOptions.ScalarStyle.DOUBLE_QUOTED))
                    .collect(Collectors.joining(", "))
                + "]";
        // Replace the retained entry, remove the rest: unrelated lines remain verbatim.
        int retainedEnd = offset(yamlRaw, retained.getValueNode().getEndMark());
        String ending =
            retainedEnd > 0 && yamlRaw.charAt(retainedEnd - 1) == '\n'
                ? (retainedEnd > 1 && yamlRaw.charAt(retainedEnd - 2) == '\r' ? "\r\n" : "\n")
                : "";
        replacements.add(
            new Replacement(
                offset(yamlRaw, retained.getKeyNode().getStartMark()),
                retainedEnd,
                dumpScalar(base, DumperOptions.ScalarStyle.PLAIN) + ": " + list + ending));
        for (String key : keys.subList(1, keys.size())) {
          NodeTuple removed = entries.get(key);
          replacements.add(entryRemoval(yamlRaw, removed, removed.getValueNode()));
        }
      }
      String transformed = splice(yamlRaw, replacements);
      if (!expected.equals(new Yaml().load(transformed))) {
        return new ConsolidatedProperties(
            null, Map.of(), Set.of(), "Cannot preserve authored property meanings");
      }
      return new ConsolidatedProperties(
          transformed, Map.copyOf(focuses), Set.copyOf(sourceKeys), null);
    } catch (YAMLException invalid) {
      return new ConsolidatedProperties(
          null, Map.of(), Set.of(), "Unsupported YAML: " + invalid.getMessage());
    }
  }
}

package com.odde.donut.algorithms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/** Immutable mapping-shaped YAML frontmatter backed by SnakeYAML. */
public final class Frontmatter {

  private final LinkedHashMap<String, Object> data;

  private Frontmatter(LinkedHashMap<String, Object> data) {
    this.data = data;
  }

  public static Frontmatter empty() {
    return new Frontmatter(new LinkedHashMap<>());
  }

  /** Parses a YAML mapping string; non-map or null/empty input returns {@link #empty()}. */
  @SuppressWarnings("unchecked")
  public static Frontmatter parse(String yamlText) {
    if (yamlText == null || yamlText.isBlank()) {
      return empty();
    }
    Object loaded = new Yaml().load(yamlText);
    if (!(loaded instanceof Map)) {
      return empty();
    }
    return new Frontmatter(new LinkedHashMap<>((Map<String, Object>) loaded));
  }

  /** {@code true} when there are no entries. */
  public boolean isEmpty() {
    return data.isEmpty();
  }

  /**
   * Case-insensitive lookup; returns a supported scalar string, or empty when absent or the value
   * is a list, map, null, or other unsupported shape.
   */
  public Optional<String> getString(String key) {
    return rawValueIgnoreCase(key).flatMap(FrontmatterPropertyValues::scalarStringFromYamlObject);
  }

  /**
   * Case-insensitive lookup; returns a supported scalar or one-level list value, or empty when
   * absent or unsupported.
   */
  public Optional<FrontmatterPropertyValue> getPropertyValue(String key) {
    return rawValueIgnoreCase(key).flatMap(FrontmatterPropertyValues::fromYamlObject);
  }

  /** Key names in insertion order. */
  public Set<String> keys() {
    return Set.copyOf(data.keySet());
  }

  /** Supported scalar string values in key insertion order (YAML document order). */
  public List<String> stringValuesInInsertionOrder() {
    return valueStringsInInsertionOrder(false);
  }

  /**
   * Supported scalar and list-item strings in property insertion order. List items appear in YAML
   * sequence order immediately after their property's scalar would have appeared.
   */
  public List<String> supportedValueStringsInInsertionOrder() {
    return valueStringsInInsertionOrder(true);
  }

  private List<String> valueStringsInInsertionOrder(boolean includeListItems) {
    List<String> out = new ArrayList<>();
    for (Object v : data.values()) {
      FrontmatterPropertyValues.fromYamlObject(v)
          .ifPresent(
              pv -> {
                switch (pv) {
                  case FrontmatterPropertyValue.Scalar s -> out.add(s.value());
                  case FrontmatterPropertyValue.ListItems l -> {
                    if (includeListItems) {
                      out.addAll(l.items());
                    }
                  }
                }
              });
    }
    return List.copyOf(out);
  }

  private Optional<Object> rawValueIgnoreCase(String key) {
    for (Map.Entry<String, Object> entry : data.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(key)) {
        return Optional.ofNullable(entry.getValue());
      }
    }
    return Optional.empty();
  }

  /** Case-insensitive lookup; returns items when the value is a YAML sequence, otherwise empty. */
  public Optional<List<?>> getSequenceItemsIgnoreCase(String key) {
    return rawValueIgnoreCase(key)
        .filter(List.class::isInstance)
        .map(v -> List.copyOf((List<?>) v));
  }

  /** Case-insensitive key presence (including entries whose value is null). */
  public boolean containsKeyIgnoreCase(String key) {
    for (String k : data.keySet()) {
      if (k.equalsIgnoreCase(key)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a new {@code Frontmatter} with the given key set to {@code value}. If a key exists
   * (case-insensitive), it is replaced in-place; otherwise it is appended.
   */
  public Frontmatter set(String key, String value) {
    if (value == null || value.isBlank()) {
      return this;
    }
    LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
    boolean replaced = false;
    for (Map.Entry<String, Object> entry : data.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(key)) {
        copy.put(key, value);
        replaced = true;
      } else {
        copy.put(entry.getKey(), entry.getValue());
      }
    }
    if (!replaced) {
      copy.put(key, value);
    }
    return new Frontmatter(copy);
  }

  /**
   * Returns a new {@code Frontmatter} with the given key set to a YAML sequence of {@code items}.
   * If a key exists (case-insensitive), it is replaced in-place; otherwise it is appended. An empty
   * or null list removes the key.
   */
  public Frontmatter setSequenceItems(String key, List<String> items) {
    if (items == null || items.isEmpty()) {
      return remove(Set.of(key));
    }
    LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
    boolean replaced = false;
    for (Map.Entry<String, Object> entry : data.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(key)) {
        copy.put(key, new ArrayList<>(items));
        replaced = true;
      } else {
        copy.put(entry.getKey(), entry.getValue());
      }
    }
    if (!replaced) {
      copy.put(key, new ArrayList<>(items));
    }
    return new Frontmatter(copy);
  }

  /** Returns a new {@code Frontmatter} with all keys in {@code keys} removed (case-insensitive). */
  public Frontmatter remove(Set<String> keys) {
    LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : data.entrySet()) {
      boolean drop = keys.stream().anyMatch(k -> k.equalsIgnoreCase(entry.getKey()));
      if (!drop) {
        copy.put(entry.getKey(), entry.getValue());
      }
    }
    return new Frontmatter(copy);
  }

  /**
   * Returns note content with a leading {@code ---} fenced YAML block when non-empty, otherwise
   * returns {@code body} unchanged.
   */
  public String fenced(String body) {
    if (data.isEmpty()) {
      return body;
    }
    DumperOptions opts = new DumperOptions();
    opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    opts.setExplicitStart(false);
    opts.setExplicitEnd(false);
    String yaml = new Yaml(opts).dump(data);
    return "---\n" + yaml + "---\n" + body;
  }
}

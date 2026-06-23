package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
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
   * Applies {@code transform} to every supported scalar or list-item string. Entries whose
   * transformed scalar is blank, or list properties with no remaining items, are dropped.
   * Unsupported values are left unchanged. Returns empty if nothing changed.
   */
  public Optional<Frontmatter> mapStringValues(UnaryOperator<String> transform) {
    LinkedHashMap<String, Object> copy = new LinkedHashMap<>();
    boolean changed = false;
    for (Map.Entry<String, Object> entry : data.entrySet()) {
      Object original = entry.getValue();
      Optional<FrontmatterPropertyValue> parsed =
          FrontmatterPropertyValues.fromYamlObject(original);
      if (parsed.isEmpty()) {
        copy.put(entry.getKey(), original);
        continue;
      }
      FrontmatterPropertyValue propertyValue = parsed.get();
      if (propertyValue instanceof FrontmatterPropertyValue.Scalar scalar) {
        String transformed = transform.apply(scalar.value());
        if (!transformed.equals(scalar.value())) {
          changed = true;
          if (!transformed.isBlank()) {
            copy.put(entry.getKey(), transformed);
          }
        } else {
          copy.put(entry.getKey(), original);
        }
      } else if (propertyValue instanceof FrontmatterPropertyValue.ListItems listItems) {
        List<String> newItems = new ArrayList<>();
        boolean listChanged = false;
        for (String item : listItems.items()) {
          String transformed = transform.apply(item);
          if (!transformed.equals(item)) {
            listChanged = true;
          }
          if (!transformed.isBlank()) {
            newItems.add(transformed);
          } else if (!item.isBlank()) {
            listChanged = true;
          }
        }
        if (listChanged) {
          changed = true;
          if (!newItems.isEmpty()) {
            copy.put(entry.getKey(), new ArrayList<>(newItems));
          }
        } else {
          copy.put(entry.getKey(), original);
        }
      }
    }
    if (!changed) {
      return Optional.empty();
    }
    return Optional.of(new Frontmatter(copy));
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

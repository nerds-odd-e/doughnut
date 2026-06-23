package com.odde.doughnut.algorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Converts SnakeYAML-loaded values into supported {@link FrontmatterPropertyValue} shapes. */
final class FrontmatterPropertyValues {

  private FrontmatterPropertyValues() {}

  static Optional<FrontmatterPropertyValue> fromYamlObject(Object value) {
    Optional<String> scalar = scalarStringFromYamlObject(value);
    if (scalar.isPresent()) {
      return Optional.of(new FrontmatterPropertyValue.Scalar(scalar.get()));
    }
    if (!(value instanceof List<?> list)) {
      return Optional.empty();
    }
    List<String> items = new ArrayList<>();
    for (Object item : list) {
      Optional<String> itemString = scalarStringFromYamlObject(item);
      if (itemString.isEmpty()) {
        return Optional.empty();
      }
      items.add(itemString.get());
    }
    return Optional.of(new FrontmatterPropertyValue.ListItems(List.copyOf(items)));
  }

  static Optional<String> scalarStringFromYamlObject(Object value) {
    if (value == null) {
      return Optional.empty();
    }
    if (value instanceof String s) {
      return Optional.of(s);
    }
    if (value instanceof Boolean || value instanceof Number) {
      return Optional.of(value.toString());
    }
    return Optional.empty();
  }
}

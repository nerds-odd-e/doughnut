package com.odde.doughnut.entities;

public record Coordinate(Double latitude, Double longitude, String stringValue) {
  public Coordinate(String stringValue) {
    this(null, null, stringValue);
  }

  public Coordinate(Double latitude, Double longitude) {
    this(latitude, longitude, null);
  }

  public String toLocationDescription() {
    if (stringValue != null) {
      return "Location: " + stringValue;
    }
    return "Location: %s'N, %s'E".formatted(latitude, longitude());
  }
}

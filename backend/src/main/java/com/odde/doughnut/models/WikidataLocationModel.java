package com.odde.doughnut.models;

public record WikidataLocationModel(String latitude, String longitude) {
  @Override
  public String toString() {
    return "Location: " + latitude() + "'N, " + longitude() + "'E";
  }
}

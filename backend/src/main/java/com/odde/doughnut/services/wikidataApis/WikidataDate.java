package com.odde.doughnut.services.wikidataApis;

public record WikidataDate(String timeValue) {
  public String format() {
    return timeValue;
  }
}

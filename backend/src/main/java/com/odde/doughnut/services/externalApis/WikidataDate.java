package com.odde.doughnut.services.externalApis;

public record WikidataDate(String timeValue) {
  public String format() {
    return timeValue;
  }
}

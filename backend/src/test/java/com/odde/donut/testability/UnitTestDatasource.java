package com.odde.donut.testability;

/** The Unit Test datasource for tests that start outside the test profile's configuration. */
public final class UnitTestDatasource {
  public static final String DEFAULT_URL =
      "jdbc:mysql://127.0.0.1:3309/doughnut_test"
          + "?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true";

  public static final String URL_PROPERTY =
      "spring.datasource.url=${SPRING_DATASOURCE_URL:" + DEFAULT_URL + "}";

  private UnitTestDatasource() {}
}

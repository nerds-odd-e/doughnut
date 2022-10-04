package com.odde.doughnut.services.externalApis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class WikidataFieldsTest {

  @Test
  void testEnumLabel() {
    assertEquals(WikidataFields.BIRTHDAY.label, "P569");
    assertNotEquals(WikidataFields.COORDINATE_LOCATION.label, "WRONG");
  }
}

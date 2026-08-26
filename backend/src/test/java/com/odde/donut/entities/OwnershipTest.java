package com.odde.donut.entities;

import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OwnershipTest {

  @ParameterizedTest
  @MethodSource("ownerNameCases")
  void getOwnerNameReturnsNameFromUserOrCircle(String expected, OwnerSetup setup) {
    Ownership ownership = new Ownership();
    setup.apply(ownership);
    assertEquals(expected, ownership.getOwnerName());
  }

  static Stream<Arguments> ownerNameCases() {
    User user = new User();
    user.setName("John Doe");
    Circle circle = new Circle();
    circle.setName("Circle Name");
    return Stream.of(
        Arguments.of("John Doe", (OwnerSetup) o -> o.setUser(user)),
        Arguments.of("Circle Name", (OwnerSetup) o -> o.setCircle(circle)));
  }

  @Test
  void getOwnerNameReturnsNullWhenUserAndCircleAreNull() {
    Ownership ownership = new Ownership();
    assertNull(ownership.getOwnerName());
  }

  @FunctionalInterface
  interface OwnerSetup {
    void apply(Ownership ownership);
  }
}

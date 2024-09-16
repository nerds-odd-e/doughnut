package com.odde.doughnut.entities;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class OwnershipTest {
  @Test
  void getOwnerNameReturnsUserNameWhenUserIsNotNull() {
    User user = mock(User.class);
    when(user.getName()).thenReturn("John Doe");

    Ownership ownership = new Ownership();
    ownership.setUser(user);

    assertEquals("John Doe", ownership.getOwnerName());
  }

  @Test
  void getOwnerNameReturnsCircleNameWhenCircleIsNotNull() {
    Circle circle = mock(Circle.class);
    when(circle.getName()).thenReturn("Circle Name");

    Ownership ownership = new Ownership();
    ownership.setCircle(circle);

    assertEquals("Circle Name", ownership.getOwnerName());
  }

  @Test
  void getOwnerNameReturnsNullWhenUserAndCircleAreNull() {
    Ownership ownership = new Ownership();
    assertNull(ownership.getOwnerName());
  }
}

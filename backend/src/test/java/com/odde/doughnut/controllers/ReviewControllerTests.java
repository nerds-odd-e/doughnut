package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.User;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewControllerTests {
  @Test
  void shouldProceedToReviewPage() {
    User user = mock(User.class);
    when(user.getNotesInDescendingOrder()).thenReturn(new ArrayList<>());
    ReviewController controller = new ReviewController(new TestCurrentUser(user));
    Model model = mock(Model.class);
    assertEquals("review", controller.review(model));
  }
}

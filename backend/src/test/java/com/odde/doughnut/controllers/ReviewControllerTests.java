package com.odde.doughnut.controllers;

import com.odde.doughnut.models.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.ui.Model;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewControllerTests {
  @Mock Model model;

  @Test
  void shouldProceedToReviewPage() {
    User user = mock(User.class);
    when(user.getNotesInDescendingOrder()).thenReturn(new ArrayList<>());
    ReviewController controller = new ReviewController();
    assertEquals("review", controller.review(user, model));
  }
}

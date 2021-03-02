package com.odde.doughnut.controllers;

import com.odde.doughnut.entities.UserEntity;
import com.odde.doughnut.services.ModelFactoryService;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewControllerTests {
  @Test
  void shouldProceedToReviewPage() {
    UserEntity userEntity = mock(UserEntity.class);
    when(userEntity.getNotesInDescendingOrder()).thenReturn(new ArrayList<>());
    ModelFactoryService modelFactoryService = mock(ModelFactoryService.class);
    ReviewController controller = new ReviewController(new TestCurrentUser(userEntity), modelFactoryService);
    Model model = mock(Model.class);
    assertEquals("review", controller.review(model));
  }
}

package com.odde.doughnut.controllers;

import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.ModelFactoryService;
import com.odde.doughnut.testability.TimeTraveler;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewControllerTests {
  @Test
  void shouldProceedToReviewPage() {
    UserModel userModel = mock(UserModel.class);
    TimeTraveler timeTraveler = mock(TimeTraveler.class);
    when(userModel.getNewNotesToReview(null)).thenReturn(new ArrayList<>());
    ModelFactoryService modelFactoryService = mock(ModelFactoryService.class);
    ReviewController controller = new ReviewController(new TestCurrentUserFetcher(userModel), modelFactoryService, timeTraveler);
    Model model = mock(Model.class);
    assertEquals("reviews/initial_done", controller.review(model));
  }
}

package com.odde.doughnut.controllers;

import com.odde.doughnut.models.User;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import com.odde.doughnut.testability.DBCleaner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;

import java.nio.file.attribute.UserPrincipal;
import java.security.Principal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
class ReviewControllerTests {
  @Test
  void shouldProceedToReviewPage() {
    User user = mock(User.class);
    when(user.getNotesInDescendingOrder()).thenReturn(new ArrayList<>());
    ReviewController controller = new ReviewController(createMockUserRepository(user));
    Principal login = (UserPrincipal) () -> "1234567";
    Model model = mock(Model.class);
    assertEquals("review", controller.review(login, model));
  }

  private UserRepository createMockUserRepository(User user) {
    UserRepository userRepository = mock(UserRepository.class);
    when(userRepository.findByExternalIdentifier("1234567")).thenReturn(user);
    return userRepository;
  }

}

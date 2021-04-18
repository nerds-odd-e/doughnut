package com.odde.doughnut.examples;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.odde.doughnut.controllers.IndexController;
import com.odde.doughnut.controllers.TestCurrentUserFetcher;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.factoryServices.ModelFactoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;

import javax.transaction.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class DoughnutWithNoSpringTests {

  @Autowired private NoteRepository noteRepository;
  @Autowired private ModelFactoryService modelFactoryService;

  @Test
  void contextLoads() {
    IndexController controller = new IndexController(new TestCurrentUserFetcher(null), modelFactoryService);
    Model model = mock(Model.class);

    String home = controller.home(null, model);
    assertEquals("ask_to_login", home);
  }
}

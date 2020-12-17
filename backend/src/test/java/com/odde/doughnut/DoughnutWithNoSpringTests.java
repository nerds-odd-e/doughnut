package com.odde.doughnut;

import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.testability.DBCleaner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
class DoughnutWithNoSpringTests {

    @Autowired
    private NoteRepository noteRepository;

    @Test
    void contextLoads() {
        IndexController controller = new IndexController(noteRepository);
        Model model = mock(Model.class);

        String home = controller.home(null, model);
        assertEquals("login", home);
    }

}

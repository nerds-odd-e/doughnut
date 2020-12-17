package com.odde.doughnut;

import com.odde.doughnut.repositories.NoteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class DoughnutWithNoSpringTests {

	@Test
	void contextLoads() {
		String path = "/src/test/resources/repository.xml";

		ApplicationContext context = new FileSystemXmlApplicationContext(path);

		NoteRepository noteRepository = context.getBean("noteRepository", NoteRepository.class);

		IndexController controller = new IndexController(noteRepository);
		Model model = mock(Model.class);

		String home = controller.home(null, model);
		assertEquals("", home);
	}

}

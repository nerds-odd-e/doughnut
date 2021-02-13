package com.odde.doughnut;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.testability.ApplicationContextWithRepositories;
import com.odde.doughnut.testability.DBCleaner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(ApplicationContextWithRepositories.class)
@ExtendWith(DBCleaner.class)
class DoughnutWithRepositoryOnlyTests {

    private ApplicationContext applicationContext;

    public DoughnutWithRepositoryOnlyTests(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


//    @Test
    void contextLoads() {

        NoteRepository noteRepository = applicationContext.getBean("noteRepository", NoteRepository.class);
        Note note = new Note();
        note.setTitle("sss");
        noteRepository.save(note);
        assertEquals(1, noteRepository.count());
    }

}

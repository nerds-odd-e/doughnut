package com.odde.doughnut.examples;

import com.odde.doughnut.models.Note;
import com.odde.doughnut.models.Link;
import com.odde.doughnut.repositories.LinkRepository;
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
    private NoteRepository noteRepository;
    private LinkRepository linkRepository;

    public DoughnutWithRepositoryOnlyTests(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        this.noteRepository = applicationContext.getBean("noteRepository", NoteRepository.class);
        this.linkRepository = applicationContext.getBean("linkRepository", LinkRepository.class);
    }


    @Test
    void contextLoads() {
        saveNote("sss");
        assertEquals(1, noteRepository.count());
    }

    private void saveNote(String title) {
        Note note = new Note();
        note.setTitle(title);
        this.noteRepository.save(note);
    }

    @Test
    void contextLoadsLinkTable() {
        saveNote("sss");
        saveNote("ttt");
        Link link = new Link();
        link.setSourceId(1);
        link.setTargetId(2);
        linkRepository.save(link);
        assertEquals(1, linkRepository.count());
    }

}

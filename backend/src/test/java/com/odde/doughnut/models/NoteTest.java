package com.odde.doughnut.models;

import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import com.odde.doughnut.testability.DBCleaner;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional

public class NoteTest {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    void shouldReturnNoteWithLinkedNotes() {
        Session session = sessionFactory.openSession();

        User user = new User();
        user.setExternalIdentifier("hello");
        user.setName("my name");

        Note targetNote = new Note();
        targetNote.setTitle("Target Note");
        targetNote.setUser(user);
        targetNote.setCreatedDatetime(Date.valueOf(LocalDate.now().minusDays(1)));
        session.save(targetNote);

        Note sourceNote = new Note();
        sourceNote.setTitle("Source Note");
        sourceNote.setUser(user);

        sourceNote.setCreatedDatetime(Date.valueOf(LocalDate.now()));
        session.save(sourceNote);

        sourceNote.linkToNote(targetNote);

        Link link = new Link();
        link.setSourceNote(sourceNote);
        link.setTargetNote(targetNote);

        session.save(link);
        user = userRepository.findByExternalIdentifier("hello");
        List<Note> notes = user.getNotesInDescendingOrder();

        assertEquals(2, notes.size());
        assertEquals(1, notes.get(0).getTargetNotes().size());

    }


}

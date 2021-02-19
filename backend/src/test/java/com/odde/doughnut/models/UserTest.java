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

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional

public class UserTest {
    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionFactory sessionFactory;
    @Test
    void shouldReturnEmptyListWhenThhereIsNoNode() {
        User user = new User();
        assertEquals(0, user.getNotesInDescendingOrder().size());
    }

    @Test
    void shouldReturnTheNoteWhenThereIsOne() {
        Session session = sessionFactory.openSession();

        User user = new User();
        user.setExternalIdentifier("hello");
        user.setName("my name");

        Note note = new Note();
        note.setTitle("Title A");
        note.setUser(user);
        session.save(note);

        user = userRepository.findByExternalIdentifier("hello");
        assertEquals(note.getTitle(), user.getNotesInDescendingOrder().get(0).getTitle());
    }

    @Test
    void shouldReturnTheNoteWhenThereIsTwo() {
        Session session = sessionFactory.openSession();

        User user = new User();
        user.setExternalIdentifier("hello");
        user.setName("my name");

        Note note1 = new Note();
        note1.setTitle("Title A");
        note1.setUser(user);
        note1.setUpdatedDatetime(Date.valueOf(LocalDate.now().minusDays(1)));
        session.save(note1);

        Note note2 = new Note();
        note2.setTitle("Title B");
        note2.setUser(user);
        note2.setUpdatedDatetime(Date.valueOf(LocalDate.now()));
        session.save(note2);


        user = userRepository.findByExternalIdentifier("hello");
        assertEquals(note2.getTitle(), user.getNotesInDescendingOrder().get(0).getTitle());
    }
}
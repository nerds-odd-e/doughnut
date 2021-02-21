package com.odde.doughnut.models;

import com.odde.doughnut.controllers.IndexController;
import com.odde.doughnut.repositories.NoteRepository;
import com.odde.doughnut.repositories.UserRepository;
import com.odde.doughnut.testability.DBCleaner;
import com.odde.doughnut.testability.MakeMe;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
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

    @Autowired private NoteRepository noteRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SessionFactory sessionFactory;

    Session session;
    private MakeMe makeMe;

    @BeforeEach
    void setup() {
        session = sessionFactory.openSession();
        makeMe = new MakeMe(session);
    }

    @Test
    void shouldReturnNoteWithLinkedNotes() {

        User user = makeMe.aUser().please();

        Note targetNote = makeMe.aNote().forUser(user).updatedAt(Date.valueOf(LocalDate.now().minusDays(1))).please();

        Note sourceNote = makeMe.aNote().forUser(user).updatedAt(Date.valueOf(LocalDate.now())).please();

        sourceNote.linkToNote(targetNote);

        session.refresh(user);
        List<Note> notes = user.getNotesInDescendingOrder();

        assertEquals(2, notes.size());
        assertEquals(1, notes.get(0).getTargetNotes().size());

    }


}

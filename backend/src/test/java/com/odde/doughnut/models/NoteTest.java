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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@ExtendWith(DBCleaner.class)
@Transactional

public class NoteTest {
    @Autowired private SessionFactory sessionFactory;

    Session session;
    MakeMe makeMe;
    User user;

    @BeforeEach
    void setup() {
        session = sessionFactory.openSession();
        makeMe = new MakeMe(session);
        user = makeMe.aUser().with2Notes().please();
    }

    @Test
    void thereShouldBe2NodesForUser() {
        List<Note> notes = user.getNotesInDescendingOrder();
        assertThat(notes, hasSize(equalTo(2)));
    }

    @Test
    void targetIsEmptyByDefault() {
        Note note = user.getNotes().get(0);
        assertThat(note.getTargetNotes(), is(empty()));
    }

    @Test
    void shouldReturnNoteWithLinkedNotes() {
        Note note = user.getNotes().get(0);
        Note targetNote = user.getNotes().get(1);
        note.linkToNote(targetNote);
        List<Note> targetNotes = note.getTargetNotes();
        assertThat(targetNotes, hasSize(equalTo(1)));
        assertThat(targetNotes, contains(targetNote));
    }
}
